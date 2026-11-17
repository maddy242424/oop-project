package com.urbanflow.model;

public class Car extends Vehicle {

    /** Public no-argument constructor: chains to the protected one below with this(...). */
    public Car() {
        this(VehicleType.CAR, "C-");
    }

    /** Protected: used by subclasses such as ElectricCar (multilevel inheritance). */
    protected Car(VehicleType type, String idPrefix) {
        super(type, idPrefix); // calls Vehicle's constructor
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
