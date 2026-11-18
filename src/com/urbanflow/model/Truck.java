package com.urbanflow.model;

/**
 * A heavy vehicle (priority 0, the lowest). Can only use a HEAVY parking slot.
 *
 * OOP CONCEPTS SHOWN: inheritance + overriding. Because getPriority() returns 0,
 * trucks go to the back of every parking waiting queue (see Vehicle.compareTo).
 */
public class Truck extends Vehicle {
    public Truck() {
        super(VehicleType.TRUCK, "T-");
    }

    /** Trucks are the least urgent in parking queues. */
    @Override
    public int getPriority() {
        return 0;
    }
}
