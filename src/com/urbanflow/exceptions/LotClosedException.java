package com.urbanflow.exceptions;

/**
 * Thrown when a lot is closed because of an accident in its area.
 *
 * THROWN BY: ParkingLot.park(v) when the lot is closed (ambulances are still allowed in).
 * CAUGHT BY: ParkingSubsystem.findParking() in a multi-catch together with
 *            ParkingFullException: catch (ParkingFullException | LotClosedException e).
 */
public class LotClosedException extends CityException {
    public LotClosedException(String lotName) {
        super(lotName + " is closed (accident in the area)");
    }
}
