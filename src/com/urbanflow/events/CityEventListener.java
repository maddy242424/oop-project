package com.urbanflow.events;

/**
 * Observer pattern: any class that wants to react to city events implements this
 * and subscribes itself to the EventBus.
 */
public interface CityEventListener {
    void onEvent(CityEvent event);
}
