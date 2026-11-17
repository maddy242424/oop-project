package com.urbanflow.parking;

/** Strategy pattern: how a lot calculates its hourly price. */
public interface PricingStrategy {
    /**
     * @param occupancy how full the lot is, from 0.0 (empty) to 1.0 (full)
     * @param peak      true during rush hour
     */
    double pricePerHour(SlotType type, double occupancy, boolean peak);
}
