package com.urbanflow.model;

/**
 * The kinds of vehicle in the city.
 *
 * HOW IT CONNECTS
 *   - Every Vehicle stores its type; SlotType.allowedFor(type) uses it to pick parking slots;
 *     the GUI uses it to choose a colour (CityMapPanel.colorFor).
 *
 * VIVA TIP: an enum is a fixed set of named constants - safer than using Strings like "CAR".
 */
public enum VehicleType {
    BIKE, CAR, EV, TRUCK, AMBULANCE
}
