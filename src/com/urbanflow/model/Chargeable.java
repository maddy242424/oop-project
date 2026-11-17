package com.urbanflow.model;

/** Implemented by vehicles that can use an EV charging slot. */
public interface Chargeable {
    int getBatteryPercent();

    void charge(int percent);

    /** Default method: every Chargeable gets this for free, built on the abstract method above. */
    default boolean isFullyCharged() {
        return getBatteryPercent() >= 100;
    }
}
