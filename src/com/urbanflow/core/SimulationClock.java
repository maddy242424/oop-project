package com.urbanflow.core;

import com.urbanflow.events.EventBus;
import com.urbanflow.events.EventType;

/**
 * Accelerated city clock: every real second, city time moves forward by a few
 * simulated minutes. All subsystems read this one clock, so "night" and
 * "peak hour" mean the same thing everywhere.
 *
 * Runs on its own thread (implements Runnable). Methods that read or change the
 * time are synchronized because the clock thread writes it while other threads read it.
 *
 * HOW IT CONNECTS
 *   - Created by CityController and started as the "SimClock" thread.
 *   - Every subsystem asks it isNight() / isPeakHour(); the EventBus uses timeString()
 *     to stamp each event.
 *   - It publishes NIGHT_STARTED and DAY_STARTED when the time crosses 19:00 / 06:00.
 *
 * OOP CONCEPTS SHOWN
 *   - implements Runnable: the TASK (this class) is separate from the THREAD that runs it.
 *   - synchronized methods, and a volatile stop flag.
 *
 * VIVA TIP: "Why Runnable and not extends Thread?" - It keeps the task separate from the
 * thread, and the class stays free to extend another class if it ever needs to.
 */
public class SimulationClock implements Runnable {
    private int minuteOfDay;              // 0 .. 1439
    private int elapsedMinutes = 0;       // total simulated minutes since start
    private final int simMinutesPerSecond;
    private EventBus bus;
    private Thread worker;
    private volatile boolean running = true;

    public SimulationClock(int startHour, int simMinutesPerSecond) {
        this.minuteOfDay = startHour * 60;
        this.simMinutesPerSecond = simMinutesPerSecond;
    }

    /** The bus is created after the clock (the bus needs the clock for timestamps). */
    public void setEventBus(EventBus bus) {
        this.bus = bus;
    }

    public void start() {
        worker = new Thread(this, "SimClock");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
            boolean wasNight = isNight();
            synchronized (this) {
                minuteOfDay = (minuteOfDay + simMinutesPerSecond) % 1440;
                elapsedMinutes += simMinutesPerSecond;
            }
            boolean nowNight = isNight();
            if (bus != null && !wasNight && nowNight) {
                bus.publish(EventType.NIGHT_STARTED, "Clock", "City", "street lights on");
            } else if (bus != null && wasNight && !nowNight) {
                bus.publish(EventType.DAY_STARTED, "Clock", "City", "street lights off");
            }
        }
    }

    public synchronized int getHour() {
        return minuteOfDay / 60;
    }

    /** Night is 19:00 to 05:59. */
    public synchronized boolean isNight() {
        int h = minuteOfDay / 60;
        return h >= 19 || h < 6;
    }

    /** Rush hours: 08:00-10:59 and 17:00-19:59. */
    public synchronized boolean isPeakHour() {
        int h = minuteOfDay / 60;
        return (h >= 8 && h <= 10) || (h >= 17 && h <= 19);
    }

    public synchronized String timeString() {
        return String.format("%02d:%02d", minuteOfDay / 60, minuteOfDay % 60);
    }

    public synchronized int getElapsedMinutes() {
        return elapsedMinutes;
    }

    public int getSimMinutesPerSecond() {
        return simMinutesPerSecond;
    }

    public void shutdown() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }
}
