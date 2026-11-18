package com.urbanflow.exceptions;

/**
 * Thrown when a vehicle tries to enter an intersection that is blocked by an accident.
 *
 * THROWN BY: Intersection.arrive(v, from) when the junction is blocked
 *            (never for ambulances - isEmergency() lets them through).
 * CAUGHT BY: TrafficSubsystem.tick() - the car is counted as "diverted" (took another road).
 */
public class RoadBlockedException extends CityException {
    public RoadBlockedException(String intersectionId) {
        super(intersectionId + " is blocked by an accident");
    }
}
