package com.urbanflow.core;

import com.urbanflow.events.CityEventListener;
import com.urbanflow.events.EventBus;

/**
 * Template for every subsystem (Template Method pattern).
 *
 * Each subsystem IS a thread (extends Thread). Its run() loop is fixed here:
 *      repeat: tick() -> sleep(tickMillis)
 * Subclasses only fill in tick() (what to do each step), onEvent() (how to react to
 * events from other subsystems) and statusReport().
 *
 * HOW IT CONNECTS
 *   - Parent of TrafficSubsystem, LightingSubsystem and ParkingSubsystem.
 *   - Holds the shared EventBus and SimulationClock (protected, so subclasses can use them).
 *   - Because it implements CityEventListener, every subsystem can subscribe to the bus.
 *
 * OOP CONCEPTS SHOWN
 *   - Abstract class: has a constructor and fields, plus abstract methods (tick, onEvent).
 *   - Inheritance: extends Thread (single inheritance of a class).
 *   - Multiple inheritance through interfaces: implements Subsystem AND CityEventListener.
 *   - final method: run() cannot be overridden, so every subsystem uses the same loop.
 *   - volatile: the "running" flag is written by one thread and read by another.
 *
 * VIVA TIP: "Can an abstract class have a constructor?" - Yes. It cannot be instantiated
 * with new, but its constructor runs when a subclass calls super(...).
 */
public abstract class AbstractSubsystem extends Thread implements Subsystem, CityEventListener {
    protected final EventBus bus;
    protected final SimulationClock clock;
    private final long tickMillis;
    private volatile boolean running = true; // volatile: other threads see the change at once

    protected AbstractSubsystem(String name, EventBus bus, SimulationClock clock, long tickMillis) {
        super(name);          // the thread's name, also returned by getName()
        this.bus = bus;
        this.clock = clock;
        this.tickMillis = tickMillis;
        setDaemon(true);
    }

    /** One simulation step. Called again and again on this subsystem's own thread. */
    protected abstract void tick();

    @Override
    public final void run() {
        while (running) {
            try {
                tick();
            } catch (RuntimeException ex) {
                // keep the simulation alive even if one step fails
                System.err.println(Thread.currentThread().getName() + " tick error: " + ex);
                ex.printStackTrace();
            }
            try {
                Thread.sleep(tickMillis);
            } catch (InterruptedException e) {
                return; // shutdown() interrupts the sleep
            }
        }
    }

    @Override
    public void shutdown() {
        running = false;
        interrupt();
    }
}
