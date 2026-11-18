package com.urbanflow.model;

import java.util.Random;

/**
 * Multilevel inheritance: Vehicle -> Car -> ElectricCar.
 * It is also Chargeable (a class can extend one class and implement interfaces).
 *
 * OOP CONCEPTS SHOWN
 *   - Multilevel inheritance: ElectricCar -> Car -> Vehicle.
 *   - Combining a class and an interface: extends Car implements Chargeable.
 *   - super(...) calls Car's PROTECTED constructor; super.getPriority() reuses Car's method.
 *   - static Random shared by all ElectricCars.
 *   - synchronized methods: battery is changed by the parking thread and read by others.
 *
 * VIVA TIP: constructor order for new ElectricCar() is Vehicle -> Car -> ElectricCar
 * (top of the hierarchy first).
 */
public class ElectricCar extends Car implements Chargeable {
    private static final Random RANDOM = new Random();
    private int battery;

    public ElectricCar() {
        super(VehicleType.EV, "E-"); // Car's protected constructor, which calls Vehicle's
        battery = 10 + RANDOM.nextInt(80); // arrives with 10% .. 89%
    }

    /** Overrides Car.getPriority(): an EV with a nearly empty battery is more urgent. */
    @Override
    public synchronized int getPriority() {
        return battery < 20 ? 2 : super.getPriority(); // super: reuse Car's normal priority
    }

    @Override
    public synchronized int getBatteryPercent() {
        return battery;
    }

    @Override
    public synchronized void charge(int percent) {
        battery = Math.min(100, battery + percent);
    }
}
