package com.urbanflow.core;

/**
 * Contract that every city subsystem (traffic, lighting, parking) follows.
 * The controller only talks to subsystems through this interface (abstraction).
 *
 * HOW IT CONNECTS
 *   - Implemented by AbstractSubsystem, so Traffic, Lighting and Parking all are Subsystems.
 *   - CityController keeps an ArrayList<Subsystem> and calls start(), shutdown() and
 *     statusReport() on each one without knowing which concrete class it is.
 *
 * OOP CONCEPTS SHOWN
 *   - Interface = a contract (only method signatures).
 *   - Polymorphism: one loop "for (Subsystem s : subsystems) s.start();" works for all three.
 *
 * VIVA TIP: getName() and start() are never written in AbstractSubsystem - they are
 * INHERITED from java.lang.Thread, and that is enough to satisfy this interface.
 */
public interface Subsystem {
    String getName();       // already provided by java.lang.Thread
    void start();           // already provided by java.lang.Thread: starts the thread
    void shutdown();        // stop cleanly
    String statusReport();  // a few lines of text for the dashboard
}
