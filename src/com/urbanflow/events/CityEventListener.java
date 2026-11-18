package com.urbanflow.events;

/**
 * Observer pattern: any class that wants to react to city events implements this
 * and subscribes itself to the EventBus.
 *
 * HOW IT CONNECTS
 *   - AbstractSubsystem implements it, so Traffic, Lighting and Parking are all listeners.
 *   - The EventBus stores listeners in a list and calls listener.onEvent(event) on each.
 *
 * OOP CONCEPTS SHOWN
 *   - Interface reference + runtime polymorphism: the bus calls onEvent() through the
 *     interface, and Java runs the version of whichever subsystem the object really is.
 */
public interface CityEventListener {
    void onEvent(CityEvent event);
}
