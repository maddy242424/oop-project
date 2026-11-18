package com.urbanflow.model;

/**
 * Implemented by vehicles that can use an EV charging slot.
 *
 * HOW IT CONNECTS
 *   - ElectricCar implements it.
 *   - ParkingLot.tick() checks "v instanceof Chargeable", downcasts, and calls charge(10)
 *     and isFullyCharged().
 *
 * OOP CONCEPTS SHOWN
 *   - Interface with abstract methods AND a default method (Java 8+).
 *   - A default method has a body; implementing classes get it for free but may override it.
 */
public interface Chargeable {
    int getBatteryPercent();

    void charge(int percent);

    /** Default method: every Chargeable gets this for free, built on the abstract method above. */
    default boolean isFullyCharged() {
        return getBatteryPercent() >= 100;
    }
}
