package com.urbanflow.lighting;

import com.urbanflow.core.AbstractSubsystem;
import com.urbanflow.core.SimulationClock;
import com.urbanflow.events.CityEvent;
import com.urbanflow.events.EventBus;
import com.urbanflow.events.EventType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Smart street lighting. Every tick each light gets a mode, in this order of importance:
 *   accident in its area -> FLASHING (warning)
 *   ambulance in its area -> FULL     (light the emergency route)
 *   daytime               -> OFF
 *   night                 -> DIM, or FULL for a moment when a car passes (motion sensor)
 * Broken lights are reported to the RepairCrew.
 */
public class LightingSubsystem extends AbstractSubsystem {
    private final ArrayList<StreetLight> lights = new ArrayList<>();
    private final RepairCrew crew;

    // filled by events from other subsystems (bus thread), read by tick (lighting thread)
    private final HashSet<String> accidentAreas = new HashSet<>();
    private final HashMap<String, Integer> ambulanceTicks = new HashMap<>(); // area -> ticks left
    private final HashMap<String, Integer> carsPassing = new HashMap<>();    // area -> cars since last tick
    private int faultsReported = 0;

    public LightingSubsystem(EventBus bus, SimulationClock clock) {
        super("Lighting", bus, clock, 1000); // one tick per second
        this.crew = new RepairCrew(bus);
        bus.subscribe(this, EventType.VEHICLES_PASSING, EventType.ACCIDENT_REPORTED,
                EventType.ROAD_REOPENED, EventType.AMBULANCE_DISPATCHED, EventType.AMBULANCE_MOVED);
    }

    /** Called during setup, before start(). */
    public void addLight(String id, String area) {
        lights.add(new StreetLight(id, area));
    }

    @Override
    public void start() {
        crew.start();
        super.start(); // start this subsystem's own thread (Thread.start)
    }

    @Override
    public void shutdown() {
        crew.shutdown();
        super.shutdown();
    }

    // ---------------------------------------------------------------- events from others
    @Override
    public synchronized void onEvent(CityEvent e) {
        switch (e.getType()) {
            case VEHICLES_PASSING:
                int count = (Integer) e.getPayload();
                Integer old = carsPassing.get(e.getArea());
                carsPassing.put(e.getArea(), (old == null ? 0 : old) + count);
                break;
            case ACCIDENT_REPORTED:
                accidentAreas.add(e.getArea());
                break;
            case ROAD_REOPENED:
                accidentAreas.remove(e.getArea());
                break;
            case AMBULANCE_DISPATCHED:
            case AMBULANCE_MOVED:
                ambulanceTicks.put(e.getArea(), 6); // full brightness for the next 6 seconds
                break;
            default:
                break;
        }
    }

    // ---------------------------------------------------------------- main loop
    @Override
    protected void tick() {
        boolean night = clock.isNight();
        double hoursPerTick = clock.getSimMinutesPerSecond() / 60.0; // 1 tick = 1 real second
        ArrayList<StreetLight> broken = new ArrayList<>();

        synchronized (this) {
            for (StreetLight light : lights) {
                String area = light.getArea();
                LightMode wanted;
                if (accidentAreas.contains(area)) {
                    wanted = LightMode.FLASHING;
                } else if (ambulanceTicks.containsKey(area)) {
                    wanted = LightMode.FULL;
                } else if (!night) {
                    wanted = LightMode.OFF;
                } else {
                    wanted = LightMode.DIM;
                    // each passing car has a chance of going by this particular pole
                    Integer cars = carsPassing.get(area);
                    if (cars != null && Math.random() < cars * 0.12) {
                        light.vehicleNearby();
                    }
                }
                light.applyMode(wanted);
                if (light.tick(hoursPerTick, night)) {
                    broken.add(light);
                    faultsReported++;
                }
            }
            countDownAmbulanceTicks();
            carsPassing.clear();
        }

        // report outside our lock (the crew has its own lock)
        for (StreetLight light : broken) {
            crew.reportFault(light);
            bus.publish(EventType.LIGHT_FAULT, light.getId(), light.getArea(), "sent to repair crew");
        }
    }

    /**
     * Uses an explicit Iterator because we REMOVE entries while looping.
     * (Removing inside a for-each loop would throw ConcurrentModificationException;
     *  iterator.remove() is the safe way.)
     */
    private void countDownAmbulanceTicks() {
        Iterator<Map.Entry<String, Integer>> it = ambulanceTicks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            int left = entry.getValue() - 1;
            if (left <= 0) {
                it.remove();          // ambulance has left this area
            } else {
                entry.setValue(left);
            }
        }
    }

    // ---------------------------------------------------------------- reporting
    public synchronized double getEnergyUsedWh() {
        double total = 0;
        for (StreetLight l : lights) total += l.getEnergyWh();
        return total;
    }

    public synchronized double getBaselineWh() {
        double total = 0;
        for (StreetLight l : lights) total += l.getBaselineWh();
        return total;
    }

    /** Percentage saved compared with old lights at full power all night. */
    public double getEnergySavedPercent() {
        double baseline = getBaselineWh();
        return baseline == 0 ? 0 : 100.0 * (1 - getEnergyUsedWh() / baseline);
    }

    public synchronized int getFaultsReported() { return faultsReported; }
    public RepairCrew getCrew() { return crew; }

    /** The list itself never changes after setup, so it is safe to hand out for reading. */
    public ArrayList<StreetLight> getLights() {
        return lights;
    }

    @Override
    public String statusReport() {
        int on = 0, faulty = 0;
        for (StreetLight l : lights) {
            LightMode m = l.getMode();
            if (m == LightMode.FAULTY) faulty++;
            else if (m != LightMode.OFF) on++;
        }
        String job = crew.getCurrentJob();
        StringBuilder sb = new StringBuilder();
        sb.append("LIGHTING\n");
        sb.append("  On: ").append(on).append('/').append(lights.size())
          .append("   Faulty now: ").append(faulty).append('\n');
        sb.append("  Repair crew: ").append(job == null ? "idle" : "fixing " + job)
          .append(", queue ").append(crew.getPendingJobs())
          .append(", fixed ").append(crew.getRepairsDone()).append('\n');
        sb.append(String.format("  Energy %.0f Wh vs %.0f Wh old lights (saved %.0f%%)%n",
                getEnergyUsedWh(), getBaselineWh(), getEnergySavedPercent()));
        return sb.toString();
    }
}
