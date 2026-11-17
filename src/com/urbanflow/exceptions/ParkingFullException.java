package com.urbanflow.exceptions;

/** Thrown when a lot has no free slot that fits the vehicle's type. */
public class ParkingFullException extends CityException {
    public ParkingFullException(String lotName, String vehicleId) {
        super(lotName + " has no free slot for " + vehicleId);
    }
}
