package com.urbanflow.parking;

import com.urbanflow.model.VehicleType;

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
