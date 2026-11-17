package com.urbanflow.exceptions;

/** Safety check: two vehicles must never be put in the same slot. */
public class SlotAlreadyTakenException extends CityException {
    public SlotAlreadyTakenException(String slotId) {
        super("Slot " + slotId + " is already occupied");
    }
}
