package com.urbanflow.exceptions;

/**
 * Base class of all UrbanFlow errors. It is a CHECKED exception (extends Exception),
 * so the compiler forces callers to handle it with try/catch or declare it with throws.
 */
public class CityException extends Exception {
    public CityException(String message) {
        super(message);
    }
}
