package com.urbanflow.events;

/**
 * Every kind of message that can travel on the EventBus.
 * The boolean says whether the event is shown in the event log
 * (very frequent events like VEHICLES_PASSING are hidden to avoid flooding it).
 *
 * HOW IT CONNECTS
 *   - Every CityEvent carries one of these types.
 *   - Subsystems subscribe to the types they care about: bus.subscribe(this, TYPE1, TYPE2...)
 *   - Each subsystem's onEvent() uses a switch on the type to decide what to do.
 *
 * OOP CONCEPTS SHOWN
 *   - enum with a field, a constructor and a method (an enum is a special class).
 *
 * VIVA TIP: enum constructors are always private - you can never write new EventType(...).
 */
public enum EventType {
    VEHICLES_PASSING(false),       // traffic -> lighting: cars passed an intersection (motion)
    CAR_NEEDS_PARKING(false),      // traffic -> parking: a car wants a parking slot
    CAR_REROUTED(true),            // parking: preferred lot full/closed, car sent to another lot
    PARKING_QUEUE_SPILLOVER(true), // parking: too many cars waiting outside a lot
    AMBULANCE_DISPATCHED(true),    // traffic -> lighting: ambulance trip started (operator pressed the button)
    AMBULANCE_MOVED(true),         // traffic -> lighting: ambulance reached the next intersection
    AMBULANCE_ARRIVED(true),       // traffic -> parking: ambulance reached the hospital
    AMBULANCE_PARKED(true),        // parking: ambulance is in the emergency bay
    ACCIDENT_REPORTED(true),       // controller -> everyone: road in an area is blocked
    ROAD_REOPENED(true),           // controller -> everyone: accident cleared
    LIGHT_FAULT(true),             // lighting: a street light broke
    LIGHT_REPAIRED(true),          // repair crew: light fixed
    SIGNAL_MODE_CHANGED(true),     // controller: fixed timer <-> adaptive
    NIGHT_STARTED(true),           // clock
    DAY_STARTED(true);             // clock

    private final boolean logged;

    EventType(boolean logged) {
        this.logged = logged;
    }

    public boolean isLogged() {
        return logged;
    }
}
