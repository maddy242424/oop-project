package com.urbanflow.exceptions;

/**
 * Safety check: two vehicles must never be put in the same slot.
 *
 * THROWN BY: ParkingSlot.occupy(v) if the slot already has a vehicle.
 * CAUGHT BY: ParkingLot.park() - it should never actually happen (park() only picks free
 *            slots), so if it does, it is turned into an IllegalStateException (a bug).
 */
public class SlotAlreadyTakenException extends CityException {
    public SlotAlreadyTakenException(String slotId) {
        super("Slot " + slotId + " is already occupied");
    }
}
