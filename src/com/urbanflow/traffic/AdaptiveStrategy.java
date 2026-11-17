package com.urbanflow.traffic;

import java.util.HashMap;

/**
 * Smart signal:
 *  1. the road with the LONGEST queue gets green next;
 *  2. green time is proportional to that road's share of all waiting cars,
 *     kept between minGreen and maxGreen;
 *  3. green ends early when the road becomes empty.
 */
public class AdaptiveStrategy implements SignalStrategy {
    private final int minGreen;
    private final int maxGreen;
    private final int cycleLength;

    public AdaptiveStrategy(int minGreen, int maxGreen, int cycleLength) {
        this.minGreen = minGreen;
        this.maxGreen = maxGreen;
        this.cycleLength = cycleLength;
    }

    @Override
    public Direction chooseNextGreen(Direction current, HashMap<Direction, Integer> queueLengths) {
        Direction best = current.next();
        for (Direction d : Direction.values()) {
            if (queueLengths.get(d) > queueLengths.get(best)) {
                best = d;
            }
        }
        return best;
    }

    @Override
    public int greenTimeFor(Direction dir, HashMap<Direction, Integer> queueLengths) {
        int total = 0;
        for (Direction d : Direction.values()) {
            total += queueLengths.get(d);
        }
        if (total == 0) {
            return minGreen;
        }
        int share = (int) Math.round(cycleLength * queueLengths.get(dir) / (double) total);
        return Math.max(minGreen, Math.min(maxGreen, share)); // clamp to [minGreen, maxGreen]
    }

    @Override
    public boolean endsGreenEarly() {
        return true;
    }

    @Override
    public String getName() {
        return "Adaptive";
    }
}
