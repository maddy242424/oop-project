package com.urbanflow.model;

public class Bike extends Vehicle {
    public Bike() {
        super(VehicleType.BIKE, "B-");
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
