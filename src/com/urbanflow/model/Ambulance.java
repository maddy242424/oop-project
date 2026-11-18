package com.urbanflow.model;

/**
 * Highest priority everywhere: gets a green wave and a reserved hospital bay.
 *
 * HOW IT CONNECTS
 *   - Created by CityController.dispatchAmbulance() (button press).
 *   - TrafficSubsystem moves it junction by junction (startDriving / drive / routeIndex).
 *   - Intersection.arrive() never blocks it because isEmergency() returns true.
 *   - ParkingSubsystem parks it in the Hospital Lot's EMERGENCY slot.
 *
 * OOP CONCEPTS SHOWN
 *   - Overriding TWO methods: getPriority() (10) and isEmergency() (true).
 *   - Runtime polymorphism: code that only knows "Vehicle v" still gets the ambulance
 *     behaviour when it calls v.isEmergency().
 */
public class Ambulance extends Vehicle {
    private final long dispatchedAtMillis;
    private int routeIndex = 0;      // index of the intersection it is at, or driving towards
    private int travelTicksLeft = 0; // > 0 while driving on the road between two intersections

    public Ambulance() {
        super(VehicleType.AMBULANCE, "AMB-");
        dispatchedAtMillis = System.currentTimeMillis();
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean isEmergency() {
        return true;
    }

    /** Start driving towards the next intersection on the route. */
    public void startDriving(int nextIndex, int ticks) {
        routeIndex = nextIndex;
        travelTicksLeft = ticks;
    }

    /** One tick of driving. @return true when it has reached the intersection */
    public boolean drive() {
        travelTicksLeft--;
        return travelTicksLeft <= 0;
    }

    public long getDispatchedAtMillis() { return dispatchedAtMillis; }
    public int getRouteIndex()          { return routeIndex; }
    public int getTravelTicksLeft()     { return travelTicksLeft; }
}
