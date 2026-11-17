package com.urbanflow.traffic;

import java.util.HashMap;

/** Today's "dumb" signal: every road gets the same green time, in a fixed order. */
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
