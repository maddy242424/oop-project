package com.urbanflow.parking;

import com.urbanflow.exceptions.SlotAlreadyTakenException;
import com.urbanflow.model.Vehicle;

/**
 * One parking space. Not synchronized itself: it is only ever used from inside
 * ParkingLot's synchronized methods, so the lot's lock already protects it.
 *
 * HOW IT CONNECTS
 *   - ParkingLot owns a list of slots (composition: slots do not exist without their lot).
 *   - occupant == null means the slot is free.
 *
 * OOP CONCEPTS SHOWN: encapsulation; a method that declares "throws" a user-defined
 * checked exception (SlotAlreadyTakenException).
 */
public class ParkingSlot {
    private final String id;
    private final SlotType type;
    private Vehicle occupant;     // null = free
    private int ticksParked;

    public ParkingSlot(String id, SlotType type) {
        this.id = id;
        this.type = type;
    }

    public void occupy(Vehicle v) throws SlotAlreadyTakenException {
        if (occupant != null) {
            throw new SlotAlreadyTakenException(id); // must never happen: guards against bugs
        }
        occupant = v;
        ticksParked = 0;
    }

    public Vehicle vacate() {
        Vehicle leaving = occupant;
        occupant = null;
        return leaving;
    }

    public void tick() {
        if (occupant != null) {
            ticksParked++;
        }
    }

    public boolean isFree()        { return occupant == null; }
    public Vehicle getOccupant()   { return occupant; }
    public int getTicksParked()    { return ticksParked; }
    public SlotType getType()      { return type; }
    public String getId()          { return id; }
}
