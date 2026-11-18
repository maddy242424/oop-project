package com.urbanflow.exceptions;

/**
 * Base class of all UrbanFlow errors. It is a CHECKED exception (extends Exception),
 * so the compiler forces callers to handle it with try/catch or declare it with throws.
 *
 * HOW IT CONNECTS
 *   - Parent of ParkingFullException, LotClosedException, SlotAlreadyTakenException and
 *     RoadBlockedException.
 *   - ParkingSubsystem catches "CityException e" to handle ANY of them in one catch block.
 *
 * OOP CONCEPTS SHOWN
 *   - User-defined exception; inheritance; super(message) passes the text to Exception,
 *     so e.getMessage() works.
 *
 * VIVA TIP: checked = extends Exception (must handle or declare).
 *           unchecked = extends RuntimeException (compiler does not force you).
 */
public class CityException extends Exception {
    public CityException(String message) {
        super(message);
    }
}
