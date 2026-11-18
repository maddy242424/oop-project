package com.urbanflow.traffic;

/**
 * The four roads that meet at an intersection, named by where the cars come FROM.
 *
 * HOW IT CONNECTS
 *   - Intersection keeps one queue per Direction (HashMap<Direction, LinkedList<Vehicle>>).
 *   - FixedTimerStrategy uses next() to cycle N -> E -> S -> W.
 *   - Ambulances always come from WEST (they drive west to east towards the hospital).
 *
 * OOP CONCEPTS SHOWN: an enum with its own method; values() and ordinal() are built in.
 */
public enum Direction {
    NORTH, EAST, SOUTH, WEST;

    /** Next direction clockwise (used by the fixed timer to cycle through all four). */
    public Direction next() {
        Direction[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
