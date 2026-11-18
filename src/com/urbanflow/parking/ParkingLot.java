package com.urbanflow.parking;

import com.urbanflow.exceptions.LotClosedException;
import com.urbanflow.exceptions.ParkingFullException;
import com.urbanflow.exceptions.SlotAlreadyTakenException;
import com.urbanflow.model.Ambulance;
import com.urbanflow.model.Chargeable;
import com.urbanflow.model.Vehicle;
import com.urbanflow.model.VehicleType;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * One parking lot: typed slots, a priority waiting queue, EV charging and pricing.
 *
 * Thread safety: the parking thread (tick), the event-bus thread (park) and the GUI
 * (getters) all use the same lot, so every public method is synchronized.
 *
 * HOW IT CONNECTS
 *   - Owned by ParkingSubsystem (Mall Lot in West, Hospital Lot in East).
 *   - park() throws ParkingFullException / LotClosedException -> ParkingSubsystem catches
 *     them and reroutes the car.
 *   - Uses a PricingStrategy for fees; charges EVs through the Chargeable interface.
 *   - CityMapPanel reads getSlotTypes(), getOccupantTypes(), getOccupancy(), queueLength().
 *
 * OOP CONCEPTS SHOWN
 *   - Composition (owns its ParkingSlots); a builder-style method addSlots() that returns
 *     "this" so calls can be chained: lot.addSlots(..).addSlots(..).
 *   - PriorityQueue<Vehicle>: poll() always gives the highest-priority vehicle (ambulance
 *     first, then low-battery EVs, cars/bikes, trucks last) using Vehicle.compareTo().
 *   - instanceof + downcasting to Ambulance and Chargeable.
 *   - throw / throws, and converting a checked exception into an unchecked one.
 */
public class ParkingLot {
    private static final Random RANDOM = new Random();
    private static final int AMBULANCE_STAY_TICKS = 8;

    private final String name;
    private final String area;
    private final ArrayList<ParkingSlot> slots = new ArrayList<>();
    private final PriorityQueue<Vehicle> waiting = new PriorityQueue<>(); // uses Vehicle.compareTo
    private final PricingStrategy pricing;
    private boolean closed = false;
    private double revenue = 0;
    private int carsParked = 0;

    public ParkingLot(String name, String area, PricingStrategy pricing) {
        this.name = name;
        this.area = area;
        this.pricing = pricing;
    }

    /** Setup helper: add 'count' slots of one type. Returns this so calls can be chained. */
    public synchronized ParkingLot addSlots(SlotType type, int count) {
        for (int i = 0; i < count; i++) {
            // e.g. "H-EV1", "H-EM1", "M-ST2" (two letters so EV and EMERGENCY never clash)
            slots.add(new ParkingSlot(name.charAt(0) + "-" + type.name().substring(0, 2) + (i + 1), type));
        }
        return this;
    }

    /**
     * Try to park a vehicle right now.
     * @throws LotClosedException  if the lot is closed (accident), unless it is an ambulance
     * @throws ParkingFullException if no free slot fits this vehicle type
     */
    public synchronized void park(Vehicle v) throws ParkingFullException, LotClosedException {
        if (closed && !v.isEmergency()) {
            throw new LotClosedException(name);
        }
        ParkingSlot slot = findFreeSlot(v.getType());
        if (slot == null) {
            throw new ParkingFullException(name, v.getId());
        }
        try {
            slot.occupy(v);
            carsParked++;
        } catch (SlotAlreadyTakenException e) {
            // findFreeSlot only returns free slots, so this means a programming error
            throw new IllegalStateException(e.getMessage());
        }
    }

    private ParkingSlot findFreeSlot(VehicleType type) {
        for (SlotType allowed : SlotType.allowedFor(type)) {
            for (ParkingSlot s : slots) {
                if (s.getType() == allowed && s.isFree()) {
                    return s;
                }
            }
        }
        return null;
    }

    /** Join the waiting queue (cars wait outside the gate, highest priority first). */
    public synchronized void addToWaiting(Vehicle v) {
        waiting.add(v);
    }

    /**
     * One tick: some cars leave and pay, EVs charge, and waiting cars are let in.
     * @param minutesPerTick simulated minutes in one tick (to work out the fee)
     */
    public synchronized void tick(boolean peak, int minutesPerTick) {
        double occupancy = getOccupancy();
        for (ParkingSlot s : slots) {
            s.tick();
            Vehicle v = s.getOccupant();
            if (v == null) {
                continue;
            }
            if (v instanceof Chargeable && s.getType() == SlotType.EV_CHARGING) {
                ((Chargeable) v).charge(10);
            }
            if (wantsToLeave(v, s)) {
                double hours = s.getTicksParked() * minutesPerTick / 60.0;
                revenue += pricing.pricePerHour(s.getType(), occupancy, peak) * hours;
                s.vacate();
            }
        }
        admitWaitingVehicles();
    }

    private boolean wantsToLeave(Vehicle v, ParkingSlot s) {
        if (v instanceof Ambulance) {
            return s.getTicksParked() >= AMBULANCE_STAY_TICKS;
        }
        if (v instanceof Chargeable && s.getType() == SlotType.EV_CHARGING) {
            // downcast: we know it is Chargeable, so we can call the interface's default method
            return ((Chargeable) v).isFullyCharged() && RANDOM.nextDouble() < 0.3;
        }
        return s.getTicksParked() >= 6 && RANDOM.nextDouble() < 0.04;
    }

    /** Let waiting vehicles in, highest priority first. Ones that still do not fit keep waiting. */
    private void admitWaitingVehicles() {
        ArrayList<Vehicle> stillWaiting = new ArrayList<>();
        while (!waiting.isEmpty()) {
            Vehicle v = waiting.poll();
            ParkingSlot slot = findFreeSlot(v.getType());
            if (slot != null && (!closed || v.isEmergency())) {
                try {
                    slot.occupy(v);
                    carsParked++;
                } catch (SlotAlreadyTakenException e) {
                    stillWaiting.add(v);
                }
            } else {
                stillWaiting.add(v);
            }
        }
        waiting.addAll(stillWaiting);
    }

    public synchronized double getOccupancy() {
        int used = 0;
        for (ParkingSlot s : slots) {
            if (!s.isFree()) used++;
        }
        return used / (double) slots.size();
    }

    public synchronized int freeSlotsFor(VehicleType type) {
        int free = 0;
        for (SlotType allowed : SlotType.allowedFor(type)) {
            for (ParkingSlot s : slots) {
                if (s.getType() == allowed && s.isFree()) free++;
            }
        }
        return free;
    }

    public synchronized boolean hasSlotType(SlotType type) {
        for (ParkingSlot s : slots) {
            if (s.getType() == type) return true;
        }
        return false;
    }

    /** Snapshot for the GUI: slot types, and the type of vehicle in each slot (null = free). */
    public synchronized SlotType[] getSlotTypes() {
        SlotType[] types = new SlotType[slots.size()];
        for (int i = 0; i < slots.size(); i++) types[i] = slots.get(i).getType();
        return types;
    }

    public synchronized VehicleType[] getOccupantTypes() {
        VehicleType[] types = new VehicleType[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            Vehicle v = slots.get(i).getOccupant();
            types[i] = (v == null) ? null : v.getType();
        }
        return types;
    }

    public synchronized double currentPrice(SlotType type, boolean peak) {
        return pricing.pricePerHour(type, getOccupancy(), peak);
    }

    public synchronized void setClosed(boolean closed) { this.closed = closed; }
    public synchronized boolean isClosed()  { return closed; }
    public synchronized int queueLength()   { return waiting.size(); }
    public synchronized double getRevenue() { return revenue; }
    public synchronized int getCarsParked() { return carsParked; }
    public String getName() { return name; }
    public String getArea() { return area; }

    @Override
    public synchronized String toString() {
        return String.format("%s: %.0f%% full, %d waiting, Rs.%.0f%s", name, getOccupancy() * 100,
                waiting.size(), revenue, closed ? " CLOSED" : "");
    }
}
