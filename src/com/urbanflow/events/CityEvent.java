package com.urbanflow.events;

/**
 * One immutable message on the EventBus (all fields are final and there are no setters).
 * Immutable objects are safe to share between threads without locking.
 *
 * HOW IT CONNECTS
 *   - Created inside EventBus.publish(type, source, area, payload).
 *   - Delivered to listeners' onEvent(CityEvent e); they read getType(), getArea(), getPayload().
 *   - payload is an Object, so it can hold a Vehicle, a String or an Integer
 *     (the receiver casts it back, e.g. (Vehicle) e.getPayload()).
 *
 * OOP CONCEPTS SHOWN
 *   - Encapsulation: private final fields + getters only.
 *   - final class (cannot be extended) and overriding toString() from Object.
 */
public final class CityEvent {
    private final EventType type;
    private final String source;   // who sent it, e.g. "INT-2" or "Mall Lot"
    private final String area;     // where it happened: "West", "Central" or "East"
    private final Object payload;  // optional extra data (a Vehicle, a message, a count...)
    private final String time;     // simulated time when it was published

    public CityEvent(EventType type, String source, String area, Object payload, String time) {
        this.type = type;
        this.source = source;
        this.area = area;
        this.payload = payload;
        this.time = time;
    }

    public EventType getType() { return type; }
    public String getSource()  { return source; }
    public String getArea()    { return area; }
    public Object getPayload() { return payload; }
    public String getTime()    { return time; }

    @Override
    public String toString() {
        String extra = (payload == null) ? "" : " : " + payload;
        return "[" + time + "] " + type + " (" + source + ", " + area + ")" + extra;
    }
}
