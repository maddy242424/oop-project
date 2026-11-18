package com.urbanflow.parking;

import com.urbanflow.core.AbstractSubsystem;
import com.urbanflow.core.SimulationClock;
import com.urbanflow.events.CityEvent;
import com.urbanflow.events.EventBus;
import com.urbanflow.events.EventType;
import com.urbanflow.exceptions.CityException;
import com.urbanflow.exceptions.LotClosedException;
import com.urbanflow.exceptions.ParkingFullException;
import com.urbanflow.model.Ambulance;
import com.urbanflow.model.Vehicle;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * The parking network. Decides WHICH lot a driver goes to:
 *   1. try the lot in the driver's own area (or the emptiest one if there is none);
 *   2. if it is full or closed -> try the other lots (a "reroute");
 *   3. if every lot is full -> wait in the queue of the preferred lot.
 * Ambulances arriving at the hospital go to the reserved EMERGENCY bay.
 *
 * HOW IT CONNECTS
 *   - LISTENS TO: CAR_NEEDS_PARKING and AMBULANCE_ARRIVED (from traffic),
 *                 ACCIDENT_REPORTED / ROAD_REOPENED (closes / reopens the area's lot).
 *   - PUBLISHES: CAR_REROUTED, PARKING_QUEUE_SPILLOVER, AMBULANCE_PARKED.
 *   - See UML Figure 5 (sequence diagram) for the findParking() flow.
 *
 * OOP CONCEPTS SHOWN
 *   - Exception handling drives the logic: try park() -> catch -> try the next lot.
 *   - Multi-catch: catch (ParkingFullException | LotClosedException e).
 *   - Catching the PARENT type: catch (CityException e) handles any of its subclasses.
 *   - Inheritance from AbstractSubsystem; downcasting the event payload (Vehicle) / (Ambulance).
 */
public class ParkingSubsystem extends AbstractSubsystem {
    private static final int SPILLOVER_LIMIT = 4; // this many cars waiting = queue on the road
    private static final int MAX_QUEUE = 8;       // longer than this and new drivers give up

    private final ArrayList<ParkingLot> lots = new ArrayList<>();
    private final HashSet<String> spilloverAlerted = new HashSet<>();
    private int reroutes = 0;
    private int gaveUp = 0;
    private int ambulancesParked = 0;
    private double totalResponseSeconds = 0;

    public ParkingSubsystem(EventBus bus, SimulationClock clock) {
        super("Parking", bus, clock, 1000); // one tick per second
        bus.subscribe(this, EventType.CAR_NEEDS_PARKING, EventType.AMBULANCE_ARRIVED,
                EventType.ACCIDENT_REPORTED, EventType.ROAD_REOPENED);
    }

    /** Called during setup, before start(). */
    public void addLot(ParkingLot lot) {
        lots.add(lot);
    }

    // ---------------------------------------------------------------- events from others
    @Override
    public void onEvent(CityEvent e) {
        switch (e.getType()) {
            case CAR_NEEDS_PARKING:
                findParking((Vehicle) e.getPayload(), e.getArea());
                break;
            case AMBULANCE_ARRIVED:
                parkAmbulance((Ambulance) e.getPayload());
                break;
            case ACCIDENT_REPORTED:
                setLotsClosed(e.getArea(), true);
                break;
            case ROAD_REOPENED:
                setLotsClosed(e.getArea(), false);
                break;
            default:
                break;
        }
    }

    /** The heart of "lot full -> send cars to the other lot". */
    private void findParking(Vehicle v, String area) {
        ParkingLot preferred = preferredLot(v, area);
        String reason;
        try {
            preferred.park(v);
            return;                                  // parked where the driver wanted
        } catch (ParkingFullException | LotClosedException e) {
            reason = e.getMessage();                 // remember why, for the event log
        }

        for (ParkingLot other : lots) {
            if (other == preferred) {
                continue;
            }
            try {
                other.park(v);
                addReroute();
                bus.publish(EventType.CAR_REROUTED, preferred.getName(), area,
                        v.getId() + " sent to " + other.getName() + " (" + reason + ")");
                return;
            } catch (CityException e) {
                // this lot cannot take it either: try the next one
            }
        }

        // every lot is full: wait at the preferred lot (or any open lot),
        // unless the queue is already very long - then the driver gives up and leaves
        ParkingLot queueAt = preferred.isClosed() ? firstOpenLot() : preferred;
        if (queueAt == null || queueAt.queueLength() >= MAX_QUEUE) {
            addGaveUp();
        } else {
            queueAt.addToWaiting(v);
        }
    }

    /** The lot in the driver's area; if the area has no lot, the one with most space for this vehicle. */
    private ParkingLot preferredLot(Vehicle v, String area) {
        for (ParkingLot lot : lots) {
            if (lot.getArea().equals(area)) {
                return lot;
            }
        }
        ParkingLot best = lots.get(0);
        for (ParkingLot lot : lots) {
            if (lot.freeSlotsFor(v.getType()) > best.freeSlotsFor(v.getType())) {
                best = lot;
            }
        }
        return best;
    }

    private ParkingLot firstOpenLot() {
        for (ParkingLot lot : lots) {
            if (!lot.isClosed()) {
                return lot;
            }
        }
        return null;
    }

    private void parkAmbulance(Ambulance a) {
        double seconds = (System.currentTimeMillis() - a.getDispatchedAtMillis()) / 1000.0;
        for (ParkingLot lot : lots) {
            if (lot.hasSlotType(SlotType.EMERGENCY)) {
                String where;
                try {
                    lot.park(a);   // emergency bay first, a standard slot if the bay is taken
                    where = "parked";
                } catch (CityException e) {
                    lot.addToWaiting(a); // no slot at all: priority 10 puts it at the front of the queue
                    where = "first in queue";
                }
                recordAmbulance(seconds);
                bus.publish(EventType.AMBULANCE_PARKED, lot.getName(), lot.getArea(),
                        String.format("%s %s, trip took %.1f s", a.getId(), where, seconds));
                return;
            }
        }
    }

    private void setLotsClosed(String area, boolean closed) {
        for (ParkingLot lot : lots) {
            if (lot.getArea().equals(area)) {
                lot.setClosed(closed);
            }
        }
    }

    // ---------------------------------------------------------------- main loop
    @Override
    protected void tick() {
        boolean peak = clock.isPeakHour();
        for (ParkingLot lot : lots) {
            lot.tick(peak, clock.getSimMinutesPerSecond());

            // warn once when the queue gets long enough to block the road
            if (lot.queueLength() >= SPILLOVER_LIMIT) {
                if (markSpillover(lot.getName(), true)) {
                    bus.publish(EventType.PARKING_QUEUE_SPILLOVER, lot.getName(), lot.getArea(),
                            lot.queueLength() + " cars waiting on the road");
                }
            } else {
                markSpillover(lot.getName(), false);
            }
        }
    }

    /** @return true only the first time a lot starts spilling over */
    private synchronized boolean markSpillover(String lotName, boolean spilling) {
        if (spilling) {
            return spilloverAlerted.add(lotName);
        }
        spilloverAlerted.remove(lotName);
        return false;
    }

    // ---------------------------------------------------------------- statistics
    private synchronized void addReroute() {
        reroutes++;
    }

    private synchronized void recordAmbulance(double seconds) {
        ambulancesParked++;
        totalResponseSeconds += seconds;
    }

    private synchronized void addGaveUp() {
        gaveUp++;
    }

    public synchronized int getReroutes() { return reroutes; }
    public synchronized int getGaveUp()   { return gaveUp; }
    public synchronized int getAmbulancesParked() { return ambulancesParked; }

    public synchronized double getAverageResponseSeconds() {
        return ambulancesParked == 0 ? 0 : totalResponseSeconds / ambulancesParked;
    }

    public double getTotalRevenue() {
        double total = 0;
        for (ParkingLot lot : lots) total += lot.getRevenue();
        return total;
    }

    /** The list itself never changes after setup, so it is safe to hand out for reading. */
    public ArrayList<ParkingLot> getLots() {
        return lots;
    }

    @Override
    public String statusReport() {
        StringBuilder sb = new StringBuilder("PARKING\n");
        for (ParkingLot lot : lots) {
            sb.append("  ").append(lot).append('\n');
        }
        sb.append("  Rerouted to other lot: ").append(getReroutes())
          .append(", gave up: ").append(getGaveUp()).append('\n');
        sb.append(String.format("  Ambulances: %d, avg trip %.1f s%n", getAmbulancesParked(),
                getAverageResponseSeconds()));
        return sb.toString();
    }
}
