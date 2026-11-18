package com.urbanflow.lighting;

import java.util.Random;

/**
 * One street light pole.
 *
 * Encapsulation: mode and energy are private and can only change through these methods,
 * which enforce the rules (e.g. a FAULTY light stays faulty until repair() is called).
 * Methods are synchronized because the lighting thread, the repair-crew thread and the
 * GUI thread all use the same StreetLight objects.
 *
 * HOW IT CONNECTS
 *   - LightingSubsystem owns 9 of them and calls applyMode(), vehicleNearby() and tick().
 *   - RepairCrew calls repair() on broken ones.
 *   - CityMapPanel reads getMode() to draw it.
 *
 * OOP CONCEPTS SHOWN
 *   - Encapsulation with a business rule inside the setter: applyMode() refuses to change
 *     a FAULTY light. Outside code cannot break this rule because "mode" is private.
 *   - static final constants (MAX_WATTS) and a static shared Random.
 */
public class StreetLight {
    public static final double MAX_WATTS = 150;
    private static final double FAILURE_CHANCE = 0.002; // per tick
    private static final Random RANDOM = new Random();

    private final String id;
    private final String area;
    private LightMode mode = LightMode.OFF;
    private double energyWh = 0;       // what this smart light used at night
    private double baselineWh = 0;     // what an old light at full power all night would use
    private int brightTicksLeft = 0;   // stays bright for a moment after a car passes

    public StreetLight(String id, String area) {
        this.id = id;
        this.area = area;
    }

    /** Ask the light to switch mode. A faulty light ignores this until it is repaired. */
    public synchronized void applyMode(LightMode wanted) {
        if (mode == LightMode.FAULTY) {
            return;
        }
        if (wanted == LightMode.DIM && brightTicksLeft > 0) {
            mode = LightMode.FULL;      // a car is near: brighten
        } else {
            mode = wanted;
        }
    }

    /** Motion sensor: a car is passing this pole. */
    public synchronized void vehicleNearby() {
        brightTicksLeft = 3;
    }

    /**
     * One tick: use energy, count down the brightness timer, and maybe break.
     * @return true if the light broke during this tick
     */
    public synchronized boolean tick(double hours, boolean night) {
        // Compare like with like: night-time energy only (old lights are on at full power all night)
        if (night) {
            energyWh += MAX_WATTS * mode.getBrightness() / 100.0 * hours;
            baselineWh += MAX_WATTS * hours;
        }
        if (brightTicksLeft > 0) {
            brightTicksLeft--;
        }
        if (mode != LightMode.FAULTY && mode != LightMode.OFF && RANDOM.nextDouble() < FAILURE_CHANCE) {
            mode = LightMode.FAULTY;
            return true;
        }
        return false;
    }

    /** Called by the repair crew. */
    public synchronized void repair() {
        mode = LightMode.OFF; // the lighting subsystem sets the right mode on its next tick
    }

    public synchronized LightMode getMode()      { return mode; }
    public synchronized double getEnergyWh()     { return energyWh; }
    public synchronized double getBaselineWh()   { return baselineWh; }
    public String getId()   { return id; }
    public String getArea() { return area; }
}
