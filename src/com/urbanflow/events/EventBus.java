package com.urbanflow.events;

import com.urbanflow.core.SimulationClock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * The message hub between subsystems (Observer / publish-subscribe pattern).
 *
 * Subsystems never call each other directly. They publish() events here, and the bus
 * delivers each event to every listener that subscribed to that event type.
 *
 * Threading: this is a classic PRODUCER-CONSUMER queue.
 *  - Producers: any thread that calls publish() adds to the queue and calls notify().
 *  - Consumer: the bus's own "EventBus" thread waits (wait()) until the queue has
 *    something, then takes the event and delivers it.
 * All access to the queue happens inside synchronized code, so two threads can never
 * corrupt it.
 */
public class EventBus implements Runnable {
    private final HashMap<EventType, ArrayList<CityEventListener>> listeners = new HashMap<>();
    private final LinkedList<CityEvent> queue = new LinkedList<>();
    private final LinkedList<CityEvent> recentLog = new LinkedList<>(); // newest first, for the GUI
    private static final int LOG_SIZE = 200;

    private final SimulationClock clock;
    private Thread worker;
    private volatile boolean running = true;
    private volatile boolean echoToConsole = false;

    public EventBus(SimulationClock clock) {
        this.clock = clock;
    }

    public void start() {
        worker = new Thread(this, "EventBus");
        worker.setDaemon(true); // daemon threads do not stop the program from exiting
        worker.start();
    }

    /** Register a listener for one or more event types. */
    public synchronized void subscribe(CityEventListener listener, EventType... types) {
        for (EventType type : types) {
            ArrayList<CityEventListener> list = listeners.get(type);
            if (list == null) {
                list = new ArrayList<>();
                listeners.put(type, list);
            }
            list.add(listener);
        }
    }

    /** Convenience method: builds the event and stamps it with the simulated time. */
    public void publish(EventType type, String source, String area, Object payload) {
        publish(new CityEvent(type, source, area, payload, clock.timeString()));
    }

    /** PRODUCER side: add to the queue and wake up the bus thread. */
    public synchronized void publish(CityEvent event) {
        queue.offer(event);          // LinkedList used as a queue: offer() adds at the back
        if (event.getType().isLogged()) {
            recentLog.addFirst(event);
            if (recentLog.size() > LOG_SIZE) {
                recentLog.removeLast();
            }
            if (echoToConsole) {
                System.out.println("  EVENT " + event);
            }
        }
        notify(); // wake the waiting bus thread
    }

    /** CONSUMER side: runs on the bus thread. */
    @Override
    public void run() {
        while (running) {
            CityEvent event;
            synchronized (this) {
                while (queue.isEmpty() && running) {
                    try {
                        wait(); // releases the lock and sleeps until notify()
                    } catch (InterruptedException e) {
                        return; // shutdown requested
                    }
                }
                if (!running) {
                    return;
                }
                event = queue.poll();    // poll() takes from the front (first in, first out)
            }
            // Deliver OUTSIDE the lock, so a slow listener never blocks publishers.
            deliver(event);
        }
    }

    private void deliver(CityEvent event) {
        ArrayList<CityEventListener> copy;
        synchronized (this) {
            ArrayList<CityEventListener> list = listeners.get(event.getType());
            if (list == null) {
                return;
            }
            copy = new ArrayList<>(list); // a copy, so subscribe() cannot change it mid-loop
        }
        for (CityEventListener listener : copy) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException ex) {
                // one broken listener must not stop the whole city
                System.err.println("Listener failed on " + event.getType() + ": " + ex);
            }
        }
    }

    /** Copy of the most recent logged events (newest first) for the dashboard. */
    public synchronized ArrayList<CityEvent> getRecentEvents(int max) {
        ArrayList<CityEvent> result = new ArrayList<>();
        for (CityEvent e : recentLog) {
            if (result.size() >= max) {
                break;
            }
            result.add(e);
        }
        return result;
    }

    public void setEchoToConsole(boolean echo) {
        this.echoToConsole = echo;
    }

    public void shutdown() {
        running = false;
        synchronized (this) {
            notifyAll();
        }
        if (worker != null) {
            worker.interrupt();
        }
    }
}
