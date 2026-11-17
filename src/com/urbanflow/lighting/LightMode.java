package com.urbanflow.lighting;

/** Each mode has a brightness in percent (an enum can have fields and methods). */
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
