package com.urbanflow.core;

import com.urbanflow.events.EventBus;
import com.urbanflow.events.EventType;
import com.urbanflow.lighting.LightingSubsystem;
import com.urbanflow.model.Ambulance;
import com.urbanflow.parking.DynamicPricing;
import com.urbanflow.parking.ParkingLot;
import com.urbanflow.parking.ParkingSubsystem;
import com.urbanflow.parking.SlotType;
import com.urbanflow.traffic.TrafficSubsystem;

import java.util.ArrayList;

/**
 * The central "brain" of the city (Singleton: there is exactly one city controller).
 *
 * It builds the city, starts every thread, and is the only object the GUI talks to.
 * Operator actions (ambulance, accident, signal mode) are turned into events on the
 * EventBus, and each subsystem reacts to them in its own onEvent() method.
 *
 * City layout (one main road, west to east):
 *
 *   West area            Central area          East area
 *   INT-1 --------------- INT-2 --------------- INT-3
 *   Mall Lot                                    Hospital Lot (+ emergency bay)
 *   3 street lights      3 street lights       3 street lights
 */
public final class CityController {
    public static final String[] AREAS = {"West", "Central", "East"};

    private static CityController instance;

    private final SimulationClock clock;
    private final EventBus bus;
    private final TrafficSubsystem traffic;
    private final LightingSubsystem lighting;
    private final ParkingSubsystem parking;
    private final ArrayList<Subsystem> subsystems = new ArrayList<>();

    /** private constructor: nobody outside can call "new CityController()". */
    private CityController() {
        clock = new SimulationClock(17, 10); // start 17:00, 10 simulated minutes per real second
        bus = new EventBus(clock);
        clock.setEventBus(bus);

        traffic = new TrafficSubsystem(bus, clock);
        lighting = new LightingSubsystem(bus, clock);
        parking = new ParkingSubsystem(bus, clock);
        subsystems.add(traffic);
        subsystems.add(lighting);
        subsystems.add(parking);

        buildCity();
    }

    public static synchronized CityController getInstance() {
        if (instance == null) {
            instance = new CityController();
        }
        return instance;
    }

    private void buildCity() {
        traffic.addIntersection("INT-1", "West");
        traffic.addIntersection("INT-2", "Central");
        traffic.addIntersection("INT-3", "East");

        for (String area : AREAS) {
            for (int i = 1; i <= 3; i++) {
                lighting.addLight("SL-" + area.charAt(0) + i, area);
            }
        }

        DynamicPricing pricing = new DynamicPricing();
        parking.addLot(new ParkingLot("Mall Lot", "West", pricing)
                .addSlots(SlotType.BIKE, 3)
                .addSlots(SlotType.STANDARD, 4)
                .addSlots(SlotType.EV_CHARGING, 2)
                .addSlots(SlotType.HEAVY, 1));
        parking.addLot(new ParkingLot("Hospital Lot", "East", pricing)
                .addSlots(SlotType.BIKE, 2)
                .addSlots(SlotType.STANDARD, 3)
                .addSlots(SlotType.EV_CHARGING, 1)
                .addSlots(SlotType.HEAVY, 1)
                .addSlots(SlotType.EMERGENCY, 1));
    }

    /** Start every thread: event bus, clock, and the three subsystems. */
    public void start() {
        bus.start();
        clock.start();
        traffic.setPriority(Thread.NORM_PRIORITY + 2); // signals are the most time-critical
        for (Subsystem s : subsystems) {
            s.start(); // polymorphism: each subsystem starts its own thread
        }
    }

    // ---------------------------------------------------------------- operator actions
    public void dispatchAmbulance() {
        traffic.dispatchAmbulance(new Ambulance());
    }

    public void reportAccident(String area) {
        bus.publish(EventType.ACCIDENT_REPORTED, "Controller", area, "road blocked");
    }

    public void clearAccident(String area) {
        bus.publish(EventType.ROAD_REOPENED, "Controller", area, "road cleared");
    }

    public String toggleSignalMode() {
        String mode = traffic.toggleSignalMode();
        bus.publish(EventType.SIGNAL_MODE_CHANGED, "Controller", "City", "all signals now " + mode);
        return mode;
    }

    // ---------------------------------------------------------------- reports
    public String statusText() {
        StringBuilder sb = new StringBuilder();
        sb.append("UrbanFlow  ").append(clock.timeString())
          .append(clock.isNight() ? "  NIGHT" : "  DAY")
          .append(clock.isPeakHour() ? "  (rush hour)" : "").append("\n\n");
        for (Subsystem s : subsystems) {
            sb.append(s.statusReport()).append('\n'); // polymorphism again
        }
        return sb.toString();
    }

    /** Summary shown when the simulation ends (or from the dashboard button). */
    public String endOfDayReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== UrbanFlow end-of-day report ==========\n");
        sb.append("Simulated time: ").append(clock.getElapsedMinutes() / 60).append(" h ")
          .append(clock.getElapsedMinutes() % 60).append(" min\n\n");

        sb.append("Traffic\n");
        sb.append(String.format("  Adaptive signals: %d cars, avg wait %.1f s%n",
                traffic.getPassed(true), traffic.averageWait(true)));
        sb.append(String.format("  Fixed signals:    %d cars, avg wait %.1f s%n",
                traffic.getPassed(false), traffic.averageWait(false)));
        sb.append("  Cars diverted by accidents: ").append(traffic.getDiverted()).append("\n\n");

        sb.append("Lighting\n");
        sb.append(String.format("  Energy used %.0f Wh vs %.0f Wh for old lights (%.0f%% saved)%n",
                lighting.getEnergyUsedWh(), lighting.getBaselineWh(), lighting.getEnergySavedPercent()));
        sb.append("  Faults: ").append(lighting.getFaultsReported())
          .append(", repaired: ").append(lighting.getCrew().getRepairsDone()).append("\n\n");

        sb.append("Parking\n");
        sb.append(String.format("  Revenue Rs.%.0f%n", parking.getTotalRevenue()));
        sb.append("  Cars rerouted to another lot: ").append(parking.getReroutes()).append('\n');
        sb.append("  Drivers who gave up (all lots full): ").append(parking.getGaveUp()).append('\n');
        sb.append(String.format("  Ambulances: %d dispatched, %d reached hospital, avg trip %.1f s%n",
                traffic.getAmbulanceTrips(), parking.getAmbulancesParked(),
                parking.getAverageResponseSeconds()));
        sb.append("=================================================\n");
        return sb.toString();
    }

    public void shutdown() {
        for (Subsystem s : subsystems) {
            s.shutdown();
        }
        // wait (at most 1 s each) for every subsystem thread to really finish
        for (Subsystem s : subsystems) {
            if (s instanceof Thread) {
                try {
                    ((Thread) s).join(1000);   // downcast Subsystem -> Thread, join with timeout
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        clock.shutdown();
        bus.shutdown();
    }

    public SimulationClock getClock()       { return clock; }
    public EventBus getBus()                { return bus; }
    public TrafficSubsystem getTraffic()    { return traffic; }
    public LightingSubsystem getLighting()  { return lighting; }
    public ParkingSubsystem getParking()    { return parking; }
}
