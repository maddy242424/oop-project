package com.urbanflow.parking;

/**
 * Strategy pattern: how a lot calculates its hourly price.
 *
 * HOW IT CONNECTS: implemented by DynamicPricing; every ParkingLot holds one and calls
 * pricePerHour() when a car leaves (to charge the fee) and for the price shown on the map.
 * A new pricing rule = a new class implementing this interface; ParkingLot never changes.
 */
public interface PricingStrategy {
    /**
     * @param occupancy how full the lot is, from 0.0 (empty) to 1.0 (full)
     * @param peak      true during rush hour
     */
    double pricePerHour(SlotType type, double occupancy, boolean peak);
}
