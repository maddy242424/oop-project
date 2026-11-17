package com.urbanflow.exceptions;

/** Thrown when a lot is closed because of an accident in its area. */
public class LotClosedException extends CityException {
    public LotClosedException(String lotName) {
        super(lotName + " is closed (accident in the area)");
    }
}
