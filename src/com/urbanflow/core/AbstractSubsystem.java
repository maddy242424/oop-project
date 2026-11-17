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
