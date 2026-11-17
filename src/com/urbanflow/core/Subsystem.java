package com.urbanflow.core;

/**
 * Contract that every city subsystem (traffic, lighting, parking) follows.
 * The controller only talks to subsystems through this interface (abstraction).
 */
public interface Subsystem {
    String getName();       // already provided by java.lang.Thread
    void start();           // already provided by java.lang.Thread: starts the thread
    void shutdown();        // stop cleanly
    String statusReport();  // a few lines of text for the dashboard
}
