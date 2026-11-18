package com.urbanflow.parking;

/**
 * Demand-based pricing: the base rate goes up as the lot fills and at rush hour.
 *   > 90% full: x2.0     > 70% full: x1.5     rush hour: +25%
 *
 * EXAMPLE: STANDARD slot (base Rs.30), lot 80% full, rush hour -> 30 x (1.5 + 0.25) = Rs.52.5/h
 *
 * OOP CONCEPTS SHOWN: implementing an interface; a private helper method (baseRate) that
 * outside classes cannot call; switch on an enum.
 */
public class DynamicPricing implements PricingStrategy {

    private double baseRate(SlotType type) {
        switch (type) {
            case BIKE:        return 10;
            case STANDARD:    return 30;
            case EV_CHARGING: return 50;
            case HEAVY:       return 80;
            default:          return 0;   // emergency bay is free
        }
    }

    @Override
    public double pricePerHour(SlotType type, double occupancy, boolean peak) {
        double multiplier = 1.0;
        if (occupancy > 0.9) {
            multiplier = 2.0;
        } else if (occupancy > 0.7) {
            multiplier = 1.5;
        }
        if (peak) {
            multiplier += 0.25;
        }
        return baseRate(type) * multiplier;
    }
}
