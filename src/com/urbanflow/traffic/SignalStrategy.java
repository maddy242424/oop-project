package com.urbanflow.traffic;

import java.util.HashMap;

/**
 * Strategy pattern: HOW a traffic signal decides who gets green and for how long.
 * The Intersection only knows this interface, so we can swap the strategy at runtime
 * (fixed timer vs adaptive) without changing the Intersection class.
 */
public interface SignalStrategy {
    /** Which approach should get green next. */
    Direction chooseNextGreen(Direction current, HashMap<Direction, Integer> queueLengths);

    /** How many steps (simulated seconds) of green to give it. */
    int greenTimeFor(Direction dir, HashMap<Direction, Integer> queueLengths);

    /** If true, green ends early once that road is empty (no green wasted on empty roads). */
    boolean endsGreenEarly();

    String getName();
}
