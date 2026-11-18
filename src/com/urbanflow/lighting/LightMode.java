package com.urbanflow.lighting;

/**
 * Each mode has a brightness in percent (an enum can have fields and methods).
 *
 * HOW IT CONNECTS
 *   - StreetLight stores its current LightMode; energy used = 150 W x brightness%.
 *   - CityMapPanel draws each mode differently (glow size, colour, red X when FAULTY).
 *   - See UML Figure 6 (state diagram) for how a light moves between these modes.
 */
public enum LightMode {
    OFF(0), DIM(30), FULL(100), FLASHING(60), FAULTY(0);

    private final int brightness;

    LightMode(int brightness) {
        this.brightness = brightness;
    }

    public int getBrightness() {
        return brightness;
    }
}
