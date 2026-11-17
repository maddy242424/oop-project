package com.urbanflow.traffic;

/** The four roads that meet at an intersection, named by where the cars come FROM. */
public enum Direction {
    NORTH, EAST, SOUTH, WEST;

    /** Next direction clockwise (used by the fixed timer to cycle through all four). */
    public Direction next() {
        Direction[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
