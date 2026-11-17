package com.urbanflow.model;

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
