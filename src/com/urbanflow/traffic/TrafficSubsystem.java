package com.urbanflow.traffic;

import com.urbanflow.core.AbstractSubsystem;
import com.urbanflow.core.SimulationClock;
import com.urbanflow.events.CityEvent;
import com.urbanflow.events.EventBus;
import com.urbanflow.events.EventType;
import com.urbanflow.exceptions.RoadBlockedException;
import com.urbanflow.model.Ambulance;
import com.urbanflow.model.Vehicle;
import com.urbanflow.model.VehicleFactory;

import java.util.ArrayList;
import java.util.Random;

/**
 * Runs the main road: INT-1 (West) -> INT-2 (Central) -> INT-3 (East) -> Hospital.
 *
 * Every tick (one simulated second):
 *  1. new vehicles arrive at each intersection (more at rush hour, fewer at night);
 *  2. each intersection lets cars through according to its signal strategy;
 *  3. some of the cars that passed want to park -> CAR_NEEDS_PARKING event;
 *  4. ambulances drive along the road between intersections (green wave ahead of them).
 */
public class TrafficSubsystem extends AbstractSubsystem {
    private static final double PARKING_CHANCE = 0.08;  // share of passing cars looking for parking
    public static final int AMBULANCE_TRAVEL_TICKS = 4; // ticks to drive from one junction to the next

    private final ArrayList<Intersection> intersections = new ArrayList<>(); // in route order W -> E
    private final ArrayList<Ambulance> ambulancesOnRoad = new ArrayList<>(); // driving between junctions
    private final SignalStrategy fixed = new FixedTimerStrategy(10);
    private final SignalStrategy adaptive = new AdaptiveStrategy(3, 15, 30);
    private SignalStrategy currentStrategy = adaptive;
    private final Random random = new Random();

    /**
     * Static nested class: a small helper that belongs to TrafficSubsystem.
     * "static" means it does not need a TrafficSubsystem object to exist;
     * it cannot see TrafficSubsystem's instance fields.
     */
    private static class WaitStats {
        private int carsPassed;
        private int totalWaitSteps;

        void record(int waitSteps) {
            carsPassed++;
            totalWaitSteps += waitSteps;
        }

        double average() {
            return carsPassed == 0 ? 0 : totalWaitSteps / (double) carsPassed;
        }
    }

    // statistics (guarded by synchronized methods)
    private final WaitStats fixedStats = new WaitStats();
    private final WaitStats adaptiveStats = new WaitStats();
    private int divertedByAccident;
    private int ambulanceTrips;

    public TrafficSubsystem(EventBus bus, SimulationClock clock) {
        super("Traffic", bus, clock, 500); // one tick every 0.5 s
        bus.subscribe(this, EventType.ACCIDENT_REPORTED, EventType.ROAD_REOPENED);
    }

    /** Called during setup, before start(). */
    public void addIntersection(String id, String area) {
        intersections.add(new Intersection(id, area, currentStrategy));
    }

    // ---------------------------------------------------------------- main loop
    @Override
    protected void tick() {
        double rate = clock.isPeakHour() ? 1.5 : (clock.isNight() ? 0.6 : 1.0);
        ArrayList<Ambulance> leavingJunction = new ArrayList<>();

        for (Intersection in : intersections) {
            // 1. arrivals
            int arrivals = (int) rate;
            if (random.nextDouble() < rate - arrivals) {
                arrivals++;
            }
            for (int k = 0; k < arrivals; k++) {
                try {
                    in.arrive(VehicleFactory.randomVehicle(), randomDirection());
                } catch (RoadBlockedException e) {
                    recordDiverted(); // driver takes another road
                }
            }

            // 2. signal step
            ArrayList<Vehicle> passed = in.step();

            // 3. what happens to the vehicles that crossed
            for (Vehicle v : passed) {
                if (v instanceof Ambulance) {
                    leavingJunction.add((Ambulance) v);
                } else {
                    recordPassed(in.waitedSteps(v));
                    if (random.nextDouble() < PARKING_CHANCE) {
                        bus.publish(EventType.CAR_NEEDS_PARKING, in.getId(), in.getArea(), v);
                    }
                }
            }
            if (!passed.isEmpty()) {
                bus.publish(EventType.VEHICLES_PASSING, in.getId(), in.getArea(), passed.size());
            }
        }

        // 4. ambulances
        driveAmbulances();
        for (Ambulance a : leavingJunction) {
            leaveJunction(a);
        }
    }

    /** Main road (east-west) is busier than the side roads. */
    private Direction randomDirection() {
        int r = random.nextInt(100);
        if (r < 35) return Direction.WEST;
        if (r < 70) return Direction.EAST;
        if (r < 85) return Direction.NORTH;
        return Direction.SOUTH;
    }

    // ---------------------------------------------------------------- ambulance
    /**
     * Start an ambulance trip from the west edge of the city to the hospital.
     * GREEN WAVE: the first two junctions are told straight away to hold green for it,
     * so their queues are already clearing before it gets there.
     */
    public void dispatchAmbulance(Ambulance a) {
        intersections.get(0).preempt(Direction.WEST);
        if (intersections.size() > 1) {
            intersections.get(1).preempt(Direction.WEST);
        }
        a.startDriving(0, AMBULANCE_TRAVEL_TICKS);
        synchronized (this) {
            ambulancesOnRoad.add(a);
            ambulanceTrips++;
        }
        bus.publish(EventType.AMBULANCE_DISPATCHED, "Controller", intersections.get(0).getArea(),
                a.getId() + " heading to City Hospital");
    }

    /** Move every ambulance on the road one tick; the ones that arrive join the junction queue. */
    private void driveAmbulances() {
        ArrayList<Ambulance> arrived = new ArrayList<>();
        synchronized (this) {
            for (Ambulance a : ambulancesOnRoad) {
                if (a.drive()) {
                    arrived.add(a);
                }
            }
            ambulancesOnRoad.removeAll(arrived);
        }
        for (Ambulance a : arrived) {
            try {
                intersections.get(a.getRouteIndex()).arrive(a, Direction.WEST);
            } catch (RoadBlockedException e) {
                // cannot happen: arrive() always lets emergency vehicles in
            }
        }
    }

    /** The ambulance has crossed a junction: drive to the next one, or arrive at the hospital. */
    private void leaveJunction(Ambulance a) {
        int next = a.getRouteIndex() + 1;
        if (next < intersections.size()) {
            if (next + 1 < intersections.size()) {
                intersections.get(next + 1).preempt(Direction.WEST); // keep the wave ahead of it
            }
            a.startDriving(next, AMBULANCE_TRAVEL_TICKS);
            synchronized (this) {
                ambulancesOnRoad.add(a);
            }
            Intersection target = intersections.get(next);
            bus.publish(EventType.AMBULANCE_MOVED, target.getId(), target.getArea(),
                    a.getId() + " approaching " + target.getId());
        } else {
            Intersection last = intersections.get(intersections.size() - 1);
            bus.publish(EventType.AMBULANCE_ARRIVED, last.getId(), last.getArea(), a);
        }
    }

    /**
     * For the GUI: one entry per ambulance on the road, {index of junction it is driving to,
     * ticks still to drive}.
     */
    public synchronized ArrayList<int[]> getAmbulancesOnRoad() {
        ArrayList<int[]> result = new ArrayList<>();
        for (Ambulance a : ambulancesOnRoad) {
            result.add(new int[] {a.getRouteIndex(), a.getTravelTicksLeft()});
        }
        return result;
    }

    // ---------------------------------------------------------------- events from others
    @Override
    public void onEvent(CityEvent e) {
        switch (e.getType()) {
            case ACCIDENT_REPORTED:
                setBlocked(e.getArea(), true);
                break;
            case ROAD_REOPENED:
                setBlocked(e.getArea(), false);
                break;
            default:
                break;
        }
    }

    private void setBlocked(String area, boolean blocked) {
        for (Intersection in : intersections) {
            if (in.getArea().equals(area)) {
                in.setBlocked(blocked);
            }
        }
    }

    // ---------------------------------------------------------------- signal mode
    /** Switch every intersection between Fixed and Adaptive. Returns the new mode name. */
    public synchronized String toggleSignalMode() {
        currentStrategy = (currentStrategy == adaptive) ? fixed : adaptive;
        for (Intersection in : intersections) {
            in.setStrategy(currentStrategy);
        }
        return currentStrategy.getName();
    }

    public synchronized String getSignalMode() {
        return currentStrategy.getName();
    }

    // ---------------------------------------------------------------- statistics
    private synchronized void recordPassed(int waitedSteps) {
        if (currentStrategy == adaptive) {
            adaptiveStats.record(waitedSteps);
        } else {
            fixedStats.record(waitedSteps);
        }
    }

    private synchronized void recordDiverted() {
        divertedByAccident++;
    }

    /** Average wait in signal-seconds (one intersection step = one simulated signal second). */
    public synchronized double averageWait(boolean forAdaptive) {
        return forAdaptive ? adaptiveStats.average() : fixedStats.average();
    }

    public synchronized int getPassed(boolean forAdaptive) {
        return forAdaptive ? adaptiveStats.carsPassed : fixedStats.carsPassed;
    }

    public synchronized int getDiverted()       { return divertedByAccident; }
    public synchronized int getAmbulanceTrips() { return ambulanceTrips; }

    /** The list itself never changes after setup, so it is safe to hand out for reading. */
    public ArrayList<Intersection> getIntersections() {
        return intersections;
    }

    @Override
    public String statusReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("TRAFFIC  (signal mode: ").append(getSignalMode()).append(")\n");
        for (Intersection in : intersections) {
            sb.append("  ").append(in.getId()).append(" queued=").append(in.totalQueued());
            if (in.isBlocked()) sb.append(" BLOCKED");
            if (in.isPreempted()) sb.append(" GREEN-WAVE");
            sb.append('\n');
        }
        sb.append(String.format("  Avg wait  Adaptive %.1f s (%d cars)%n", averageWait(true), getPassed(true)));
        sb.append(String.format("            Fixed    %.1f s (%d cars)%n", averageWait(false), getPassed(false)));
        sb.append("  Diverted by accidents: ").append(getDiverted()).append('\n');
        return sb.toString();
    }
}
