package com.urbanflow.traffic;

import java.util.HashMap;

/**
 * Strategy pattern: HOW a traffic signal decides who gets green and for how long.
 * The Intersection only knows this interface, so we can swap the strategy at runtime
 * (fixed timer vs adaptive) without changing the Intersection class.
 *
 * HOW IT CONNECTS
 *   - Implemented by FixedTimerStrategy and AdaptiveStrategy.
 *   - Each Intersection holds one SignalStrategy and calls it inside step().
 *   - TrafficSubsystem.toggleSignalMode() swaps the strategy for every intersection.
 *
 * OOP CONCEPTS SHOWN: interface, runtime polymorphism (the Intersection does not know
 * WHICH strategy it has - it just calls the interface methods).
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
