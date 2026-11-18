package com.urbanflow.model;

/**
 * A normal car (priority 1).
 *
 * OOP CONCEPTS SHOWN
 *   - Inheritance: Car extends Vehicle; it is also the PARENT of ElectricCar (multilevel).
 *   - Constructor overloading: Car() and Car(type, prefix).
 *   - Constructor chaining: Car() calls this(...), which calls super(...) -> Vehicle.
 *   - Overriding getPriority() with @Override.
 *
 * VIVA TIP: new Car() runs, in order: Car() -> this(CAR, "C-") -> super(...) = Vehicle's
 * constructor. The parent's constructor always finishes before the child's body continues.
 */
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
