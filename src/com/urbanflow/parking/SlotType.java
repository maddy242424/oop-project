package com.urbanflow.parking;

import com.urbanflow.model.VehicleType;

/**
 * The kinds of parking slot.
 *
 * HOW IT CONNECTS
 *   - ParkingLot.findFreeSlot() calls allowedFor(vehicleType) and tries the slot types in
 *     that order (e.g. a Bike tries BIKE first, then STANDARD).
 *   - EMERGENCY exists only in the Hospital Lot and only ambulances may use it.
 *
 * OOP CONCEPTS SHOWN: an enum with a static method; a classic switch statement on an
 * enum; arrays (SlotType[]).
 */
public enum SlotType {
    BIKE, STANDARD, EV_CHARGING, HEAVY, EMERGENCY;

    /** Which slot types a vehicle may use, in order of preference. */
    public static SlotType[] allowedFor(VehicleType vehicle) {
        switch (vehicle) {
            case BIKE:      return new SlotType[] {BIKE, STANDARD};
            case CAR:       return new SlotType[] {STANDARD};
            case EV:        return new SlotType[] {EV_CHARGING, STANDARD};
            case TRUCK:     return new SlotType[] {HEAVY};
            case AMBULANCE: return new SlotType[] {EMERGENCY, STANDARD};
            default:        return new SlotType[0];
        }
    }
}
