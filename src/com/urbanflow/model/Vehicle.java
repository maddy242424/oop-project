package com.urbanflow.model;

/**
 * Base class for everything on the road (abstraction + inheritance).
 *
 * Implements Comparable so a PriorityQueue can order vehicles:
 * higher priority first, and for equal priority, whoever came first (FIFO).
 *
 * HOW IT CONNECTS
 *   - Parent of Car, Bike, Truck, Ambulance (and ElectricCar through Car).
 *   - Intersections queue Vehicles; ParkingLots park them; the PriorityQueue in
 *     ParkingLot orders them using compareTo().
 *
 * OOP CONCEPTS SHOWN
 *   - Abstract class with an abstract method (getPriority) and concrete methods.
 *   - static field shared by ALL vehicles (nextNumber) + static synchronized method.
 *   - protected constructor: only subclasses call it, through super(...).
 *   - Encapsulation: private fields with getters.
 *   - Implementing an interface (Comparable) and overriding toString().
 *
 * VIVA TIP: "Can you write new Vehicle(...)?" - No, it is abstract. But
 * Vehicle v = new Car(); is fine (parent reference, child object = upcasting).
 */
public abstract class Vehicle implements Comparable<Vehicle> {
    private static int nextNumber = 1;    // shared by all vehicles (static)

    private final String id;
    private final VehicleType type;
    private final int number;             // creation order, used for FIFO tie-breaks
    private int queuedAtStep;             // set by an intersection when the vehicle joins a queue

    protected Vehicle(VehicleType type, String idPrefix) {
        this.type = type;
        this.number = takeNextNumber();
        this.id = idPrefix + number;
    }

    /** static synchronized: vehicles are created on several threads, so ids must stay unique. */
    private static synchronized int takeNextNumber() {
        return nextNumber++;
    }

    /** Higher number = served first in parking queues. Each subclass decides (polymorphism). */
    public abstract int getPriority();

    /** Only ambulances override this. */
    public boolean isEmergency() {
        return false;
    }

    @Override
    public int compareTo(Vehicle other) {
        if (this.getPriority() != other.getPriority()) {
            return other.getPriority() - this.getPriority(); // higher priority comes first
        }
        return this.number - other.number;                  // then first come, first served
    }

    public String getId()              { return id; }
    public VehicleType getType()       { return type; }
    public int getQueuedAtStep()       { return queuedAtStep; }
    public void setQueuedAtStep(int s) { queuedAtStep = s; }

    @Override
    public String toString() {
        return id;
    }
}
