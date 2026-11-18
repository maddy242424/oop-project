package com.urbanflow.traffic;

import java.util.HashMap;

/**
 * Today's "dumb" signal: every road gets the same green time, in a fixed order.
 *
 * Used as the BASELINE: switch to it from the dashboard and compare its average wait
 * with AdaptiveStrategy's (shown in the status panel and end-of-day report).
 *
 * OOP CONCEPTS SHOWN: implementing an interface; encapsulation (private final field set
 * once in the constructor).
 */
public class FixedTimerStrategy implements SignalStrategy {
    private final int greenSeconds;

    public FixedTimerStrategy(int greenSeconds) {
        this.greenSeconds = greenSeconds;
    }

    @Override
    public Direction chooseNextGreen(Direction current, HashMap<Direction, Integer> queueLengths) {
        return current.next(); // N -> E -> S -> W -> N ... regardless of traffic
    }

    @Override
    public int greenTimeFor(Direction dir, HashMap<Direction, Integer> queueLengths) {
        return greenSeconds;   // same time even if the road is empty
    }

    @Override
    public boolean endsGreenEarly() {
        return false;
    }

    @Override
    public String getName() {
        return "Fixed";
    }
}
