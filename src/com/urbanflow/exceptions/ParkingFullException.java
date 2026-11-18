package com.urbanflow.exceptions;

/**
 * Thrown when a lot has no free slot that fits the vehicle's type.
 *
 * THROWN BY: ParkingLot.park(v)   ("throw new ParkingFullException(...)")
 * CAUGHT BY: ParkingSubsystem.findParking() - the catch block is what sends the car to
 *            the other lot (a reroute).
 */
public class ParkingFullException extends CityException {
    public ParkingFullException(String lotName, String vehicleId) {
        super(lotName + " has no free slot for " + vehicleId);
    }
}
