package com.urbanflow.traffic;

import com.urbanflow.exceptions.RoadBlockedException;
import com.urbanflow.model.Vehicle;
import com.urbanflow.model.VehicleType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * One junction with a waiting queue (LinkedList, first in first out) for each road.
 *
 * Thread safety: the traffic thread calls step(), the event-bus thread calls preempt()
 * and setBlocked(), and the GUI thread reads the queues to draw them. Every public
 * method is synchronized, so only one thread at a time can touch the intersection.
 */
public class Intersection {
    public static final int CARS_PER_STEP = 2; // how many cars can cross per green second

    private final String id;
    private final String area;
    private final HashMap<Direction, LinkedList<Vehicle>> queues = new HashMap<>();
    private SignalStrategy strategy;

    private Direction green = Direction.EAST;
    private int greenRemaining = 0;
    private int stepCount = 0;
    private boolean blocked = false;      // accident: nobody passes except emergencies
    private int emergencyRequests = 0;    // > 0 means an ambulance is coming (green wave)
    private Direction emergencyDirection = Direction.WEST;

    public Intersection(String id, String area, SignalStrategy strategy) {
        this.id = id;
        this.area = area;
        this.strategy = strategy;
        for (Direction d : Direction.values()) {
            queues.put(d, new LinkedList<>());
        }
    }

    /** A vehicle joins the queue of the road it is coming from. */
    public synchronized void arrive(Vehicle v, Direction from) throws RoadBlockedException {
        if (blocked && !v.isEmergency()) {
            throw new RoadBlockedException(id); // ambulances are always let in
        }
        v.setQueuedAtStep(stepCount);
        queues.get(from).offer(v);   // LinkedList as a queue: join at the back
    }

    /**
     * Green wave: hold green for the ambulance's direction until it has passed.
     * Called once for each ambulance that will pass through here.
     */
    public synchronized void preempt(Direction dir) {
        emergencyRequests++;
        emergencyDirection = dir;
    }

    /**
     * Advance one simulated second.
     * @return the vehicles that crossed the junction during this second
     */
    public synchronized ArrayList<Vehicle> step() {
        stepCount++;
        ArrayList<Vehicle> passed = new ArrayList<>();

        if (emergencyRequests > 0) {
            green = emergencyDirection;          // ambulance coming: force green its way
        } else if (blocked) {
            return passed;                       // accident: all red
        } else if (greenRemaining <= 0
                || (strategy.endsGreenEarly() && queues.get(green).isEmpty())) {
            HashMap<Direction, Integer> lengths = getQueueLengths();
            green = strategy.chooseNextGreen(green, lengths);
            greenRemaining = strategy.greenTimeFor(green, lengths);
        }
        greenRemaining--;

        LinkedList<Vehicle> queue = queues.get(green);
        for (int i = 0; i < CARS_PER_STEP && queue.peek() != null; i++) { // peek(): look without removing
            Vehicle v = queue.poll();                                        // poll(): take the front car
            passed.add(v);
            if (v.isEmergency() && emergencyRequests > 0) {
                emergencyRequests--;             // this ambulance has passed: release the hold
            }
        }
        return passed;
    }

    /** How many steps a vehicle that just passed had to wait. */
    public synchronized int waitedSteps(Vehicle v) {
        return stepCount - v.getQueuedAtStep();
    }

    public synchronized HashMap<Direction, Integer> getQueueLengths() {
        HashMap<Direction, Integer> lengths = new HashMap<>();
        for (Direction d : Direction.values()) {
            lengths.put(d, queues.get(d).size());
        }
        return lengths;
    }

    /** Copy of the first few vehicle types in each queue, for drawing in the GUI. */
    public synchronized HashMap<Direction, ArrayList<VehicleType>> getQueueSnapshot(int max) {
        HashMap<Direction, ArrayList<VehicleType>> snapshot = new HashMap<>();
        for (Direction d : Direction.values()) {
            ArrayList<VehicleType> types = new ArrayList<>();
            for (Vehicle v : queues.get(d)) {
                if (types.size() >= max) {
                    break;
                }
                types.add(v.getType());
            }
            snapshot.put(d, types);
        }
        return snapshot;
    }

    public synchronized int totalQueued() {
        int total = 0;
        for (LinkedList<Vehicle> q : queues.values()) {
            total += q.size();
        }
        return total;
    }

    public synchronized void setStrategy(SignalStrategy s) {
        strategy = s;
        greenRemaining = 0; // re-decide on the next step using the new strategy
    }

    public synchronized void setBlocked(boolean blocked) { this.blocked = blocked; }
    public synchronized boolean isBlocked()      { return blocked; }
    public synchronized boolean isPreempted()    { return emergencyRequests > 0; }
    public synchronized Direction getGreen()     { return green; }
    public synchronized String getStrategyName() { return strategy.getName(); }
    public String getId()   { return id; }
    public String getArea() { return area; }

    @Override
    public synchronized String toString() {
        String state = blocked ? " BLOCKED" : "";
        if (emergencyRequests > 0) {
            state += " GREEN-WAVE";
        }
        return id + " [" + strategy.getName() + ", green=" + green + ", queues=" + getQueueLengths() + state + "]";
    }
}
