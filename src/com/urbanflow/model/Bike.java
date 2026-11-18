package com.urbanflow.model;

/**
 * A two-wheeler (priority 1). Can park in a BIKE slot or, if none is free, a STANDARD slot.
 *
 * OOP CONCEPTS SHOWN: hierarchical inheritance (one of several children of Vehicle),
 * super(...) call, overriding getPriority().
 */
public class Bike extends Vehicle {
    public Bike() {
        super(VehicleType.BIKE, "B-");
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
