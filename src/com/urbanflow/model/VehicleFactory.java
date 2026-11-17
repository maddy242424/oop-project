package com.urbanflow.model;

import java.util.Random;

/**
 * Factory pattern: the rest of the code asks for "a random vehicle" and does not need
 * to know which concrete classes exist. (Ambulances are created only by the controller.)
 */
public final class VehicleFactory {
    private static final Random RANDOM = new Random();

    private VehicleFactory() { } // utility class: no objects

    public static Vehicle randomVehicle() {
        int r = RANDOM.nextInt(100);
        if (r < 50) return new Car();          // 50%
        if (r < 75) return new Bike();         // 25%
        if (r < 92) return new ElectricCar();  // 17%
        return new Truck();                    //  8%
    }
}
