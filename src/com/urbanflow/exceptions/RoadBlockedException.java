package com.urbanflow.exceptions;

/** Thrown when a vehicle tries to enter an intersection that is blocked by an accident. */
public class RoadBlockedException extends CityException {
    public RoadBlockedException(String intersectionId) {
        super(intersectionId + " is blocked by an accident");
    }
}
