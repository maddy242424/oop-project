package com.urbanflow.lighting;

import com.urbanflow.events.EventBus;
import com.urbanflow.events.EventType;

import java.util.LinkedList;

/**
 * A maintenance crew that fixes broken lights one at a time, on its own thread.
 *
 * PRODUCER-CONSUMER with wait()/notify():
 *  - the lighting thread calls reportFault() -> adds a job and notify()
 *  - the crew thread waits (wait()) while there are no jobs, then takes the oldest
 *    job, travels/repairs for a few seconds, and marks the light fixed.
 *
 * HOW IT CONNECTS
 *   - Created and started by LightingSubsystem (its own "RepairCrew" thread).
 *   - Publishes LIGHT_REPAIRED on the EventBus when a light is fixed.
 *
 * OOP CONCEPTS SHOWN
 *   - implements Runnable; wait()/notify(); LinkedList as a queue (offer/poll).
 *   - finally: runs whether the repair finished or the thread was interrupted
 *     (even though the catch block does "return").
 *   - Thread.sleep() is done OUTSIDE the synchronized block so other threads can still
 *     report faults while the crew is busy.
 *
 * VIVA TIP: wait() RELEASES the lock while waiting; sleep() does NOT release any lock.
 */
public class RepairCrew implements Runnable {
    private static final long REPAIR_TIME_MS = 4000;

    private final LinkedList<StreetLight> jobs = new LinkedList<>();
    private final EventBus bus;
    private Thread worker;
    private volatile boolean running = true;
    private String currentJob = null;
    private int repairsDone = 0;

    public RepairCrew(EventBus bus) {
        this.bus = bus;
    }

    public void start() {
        worker = new Thread(this, "RepairCrew");
        worker.setDaemon(true);
        worker.start();
    }

    /** PRODUCER: called from the lighting thread when a light breaks. */
    public synchronized void reportFault(StreetLight light) {
        jobs.offer(light);
        notify(); // wake the crew if it is waiting
    }

    /** CONSUMER: the crew's own thread. */
    @Override
    public void run() {
        while (running) {
            StreetLight light;
            synchronized (this) {
                while (jobs.isEmpty() && running) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                if (!running) {
                    return;
                }
                light = jobs.poll();
                currentJob = light.getId();
            }
            try {
                Thread.sleep(REPAIR_TIME_MS); // travelling + fixing (outside the lock!)
            } catch (InterruptedException e) {
                return;                       // shutdown: the finally block below still runs first
            } finally {
                synchronized (this) {
                    currentJob = null;        // runs whether the repair finished or was interrupted
                }
            }
            light.repair();
            synchronized (this) {
                repairsDone++;
            }
            bus.publish(EventType.LIGHT_REPAIRED, "RepairCrew", light.getArea(), light.getId());
        }
    }

    public synchronized int getPendingJobs()   { return jobs.size(); }
    public synchronized int getRepairsDone()   { return repairsDone; }
    public synchronized String getCurrentJob() { return currentJob; }

    public void shutdown() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }
}
