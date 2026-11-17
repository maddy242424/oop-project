# UrbanFlow - Code Walkthrough and Viva Guide

COMP205 Object Oriented Programming - final project.
This guide is for YOU, the presenter. Read it top to bottom once, then use sections 9 (viva questions) and 10 (limitations) for revision.

Every class name, method name and number below was checked against the real code in `src/com/urbanflow`.
Locations are written as `package/File.java` + method name.

**Contents**

1. How to run it and a live demo script (fits in 5 minutes)
1A. How you are marked (rubric) and where UrbanFlow shows each topic, plus a 2-minute UML talk
2. The big picture (layout, diagram, threads)
3. Follow one tick (traffic, lighting, parking)
4. The three scenarios traced through the code (+ normal parking flow)
5. Class-by-class table (all 41 files)
6. OOP concept index
7. Threads and synchronization, explained simply
8. Beyond-the-syllabus features
9. Likely viva questions with model answers
10. Known simplifications and future work

---

## 1. How to run it, and what to click

### Compile and run (from the `urbanflow` folder)

```
# compile (Git Bash / Linux / Mac)
javac -d out $(find src -name "*.java")

# compile (Windows PowerShell)
javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName

# run the window (GUI)
java -cp out com.urbanflow.UrbanFlowApp

# run the console demo for 60 seconds (no window)
java -cp out com.urbanflow.UrbanFlowApp --console 60
```

Extra option (testing only): `java -cp out com.urbanflow.UrbanFlowApp --screenshot shot.png 20` opens the window, saves a picture after 20 s, then exits. You do not need it for the presentation.

### What the console demo does (`UrbanFlowApp.runConsoleDemo`)

It loops once per second and fires the operator actions on a fixed script. `phase = t % 40`:

| Second (phase) | Action |
|---|---|
| 6 | `city.dispatchAmbulance()` |
| 10 | `city.reportAccident(area)` (area is West for the first 40 s, then Central, then East) |
| 14 | `city.dispatchAmbulance()` again, this one drives through the accident area |
| 22 | `city.clearAccident(area)` |
| half of the total time | `city.toggleSignalMode()` (second half uses Fixed signals) |
| every 30 s | prints `city.statusText()` |
| at the end | prints `city.endOfDayReport()` and calls `city.shutdown()` |

`city.getBus().setEchoToConsole(true)` makes every logged event print as `EVENT [time] TYPE (source, area) : text`.

### Time facts you need for the live demo

The clock starts at 17:00 and moves 10 simulated minutes per real second (`new SimulationClock(17, 10)` in `CityController`). So **1 simulated hour = 6 real seconds**.

| Real time after launch | City time | What changes |
|---|---|---|
| 0 s | 17:00 | Rush hour (17:00-19:59). Lights are OFF (daytime). |
| about 12 s | 19:00 | `NIGHT_STARTED`. Lights go DIM. |
| about 78 s | 06:00 | `DAY_STARTED`. Lights go OFF. |
| about 90-108 s | 08:00-10:59 | Morning rush hour. |
| about 144 s | 17:00 (day 2) | Evening rush hour again. |
| about 156 s | 19:00 (day 2) | Second night starts (lasts until about 222 s). |

Night is 19:00 to 05:59 and rush hours are 08:00-10:59 and 17:00-19:59 (`SimulationClock.isNight`, `isPeakHour`).

### Suggested live demo (about 4 minutes; the limit is 5)

The demo slot is **5 minutes maximum**, so this script ends at about 4:00 and leaves a minute spare. The simulation is random, so say "you should see", not "you will see". Practise it once with a stopwatch.

| Time | You do | What to tell the audience to notice |
|---|---|---|
| 0:00 | Launch the GUI. Point at the map. | "Three areas: West, Central, East. Three signal junctions INT-1/2/3, nine street lights (small dots above the road), two parking lots. Right panel = status of each subsystem; bottom = event log. Everything talks through one EventBus." Signals start in **Adaptive** mode. It is rush hour, so queues build up. |
| 0:15 | Wait for night (about 12 s). | Background turns dark blue. Lights switch from grey (OFF) to dim yellow. Log shows `NIGHT_STARTED`. When cars pass, nearby lights flash bright for about 3 s (motion sensor). Status panel shows energy saved vs old lights. |
| 0:30 | Click **Dispatch Ambulance**. | A red "AMB" moves along the road. "GREEN WAVE" appears above INT-1 and INT-2 straight away, and above INT-3 once it passes INT-1. Street lights in West, then Central, then East go full bright for about 6 s each. It ends in the red-outlined "+" slot of the Hospital Lot. Log: `AMBULANCE_DISPATCHED`, `AMBULANCE_MOVED`, `AMBULANCE_ARRIVED`, `AMBULANCE_PARKED ... parked, trip took ~7 s`. |
| 1:00 | Leave **Area** = Central and click **Report Accident**. | Orange X and "ACCIDENT" at INT-2. Its queues freeze, its signals go red, new cars there are diverted (see "Diverted by accidents" in the status panel). The three Central lights flash orange. **Central has no parking lot, so no lot closes for a Central accident.** |
| 1:25 | Pick **West** in the Area box and click **Report Accident**. | The Mall Lot shows an orange "CLOSED" cover. Cars that wanted it appear in the log as `CAR_REROUTED ... sent to Hospital Lot (Mall Lot is closed (accident in the area))`. That is `LotClosedException`, and it is also your **Scenario 1** (reroute) moment. A plain "lot full" reroute (`... has no free slot for ...`) happens now and then on its own, mostly at the small Hospital Lot, so do not wait for it. |
| 1:50 | Click **Dispatch Ambulance** while the accidents are active. | "Ambulances are never blocked." It drives through the blocked junctions (GREEN WAVE still shows). The accident lights keep flashing: accident beats ambulance in the lighting rules. |
| 2:20 | Click **Clear Accident** for West, then pick Central and clear it. | Lot reopens, junctions resume, lights return to normal. Log shows `ROAD_REOPENED`. |
| 2:40 | Click **Switch to Fixed Signals**. Let it run about 20 s (the second evening rush hour starts at about 144 s, i.e. 2:24). | Button text flips. Every road now gets the same 10 steps of green, even empty ones, so queues grow longer. Log shows `SIGNAL_MODE_CHANGED`. |
| 3:10 | Click **End-of-Day Report**. | Compare "Adaptive signals: N cars, avg wait" with "Fixed signals". Also energy saved %, faults and repairs, revenue, reroutes, drivers who gave up, ambulance trips. Each mode only counts cars while it was active, so say "compare the averages, and note adaptive passed more cars". |
| 3:40 | Close the box, then close the window. | Closing prints the report to the console and exits cleanly. Stop talking at 4:00. |

If you are short of time, skip the 2:20 clear step: the Fixed-vs-Adaptive report matters more.

---

## 1A. How you are marked (rubric)

| Part | Marks | Who | Time limit |
|---|---|---|---|
| Project report (UML design) | 10 | team | max 2 min |
| Project demo | 10 | team | max 5 min |
| Viva | 10 | **individual** | max 2 min per person |

**The viva format:** the examiner shows you a **separate short program** (not UrbanFlow). You must explain what the code does and **predict its output**. You get **two random questions from two different topics**, 5 marks each. So you need the nine topics below in your head as small Java programs, not only as "where it is in UrbanFlow". UrbanFlow is useful as a source of your own examples ("I used this in my project, in `X.java`").

The nine topics:
1. Classes, objects, encapsulation (references, reassignment, shared objects, access modifiers, getters/setters)
2. Constructors and method overloading (default / no-arg / parameterized / copy constructors, `this()` chaining, `this`, overload resolution, type widening)
3. Static members and nested classes (static vs instance, static/instance init blocks, initialization order, static nested, inner, anonymous classes, Singleton)
4. Inheritance (single / multilevel / hierarchical, multiple via interfaces, constructor order, `super`, `final` methods)
5. Polymorphism (overloading vs overriding, `@Override`, upcasting/downcasting, reference vs object type, variable vs method resolution, static method hiding)
6. Abstract classes and interfaces (default methods, interface inheritance, combining both)
7. Arrays and collections (ArrayList, LinkedList, Iterator/ListIterator, queue `offer`/`peek`/`poll`, generics)
8. Exception handling (checked/unchecked, `finally`, multiple/nested catch, exceptions in catch/finally, `throw`/`throws`, propagation, user-defined, `return` vs `finally`)
9. Multithreading (`extends Thread` vs `Runnable`, `start()` vs `run()`, names/`currentThread()`, `sleep`, `join`, `join(timeout)`, states, priorities, sharing a Runnable)

### Rubric topic -> where UrbanFlow shows it (honest coverage)

| # | Topic | Shown in UrbanFlow (file + place) | NOT shown in the project (learn from notes) |
|---|---|---|---|
| 1 | Classes, objects, encapsulation | Private fields + getters: `StreetLight`, `CityEvent`, `ParkingSlot`. Guarded methods instead of plain setters: `StreetLight.applyMode`, `ParkingSlot.occupy`. Shared objects between threads: the same `Intersection` / `ParkingLot` object is used by the Traffic, EventBus and Swing threads. Access modifiers: `private`, `protected` (`Vehicle` and `Car` constructors, `AbstractSubsystem` fields), `public`. Getters that return a **new** collection (`Intersection.getQueueLengths`) so callers cannot change the original. | Package-private (default) access is not used on purpose. Plain reference-reassignment puzzles (`a = b`, then change `b`) are not in the code; practise them. |
| 2 | Constructors and overloading | **`this(...)` chaining:** `Car()` calls `this(VehicleType.CAR, "C-")` (`model/Car.java`). **Parameterized constructors:** `Intersection(id, area, strategy)`, `ParkingLot(name, area, pricing)`. **No-arg:** `Car()`, `Bike()`, `Ambulance()`. **`super(...)` calls** from every vehicle and subsystem. **Private constructor:** `CityController`, `VehicleFactory`. **`this` keyword** for fields: `this.id = id`. **Overloading:** `EventBus.publish(type, source, area, payload)` and `publish(CityEvent)` (same name, different parameters). | **No copy constructor.** **No type-widening overload puzzle** (e.g. `print(int)` vs `print(long)`). Make your own small examples. |
| 3 | Static members and nested classes | **Static field + method:** `Vehicle.nextNumber` and `takeNextNumber()`; `CityController.AREAS`, `instance`, `getInstance()` (**Singleton**). **Static constants:** `Intersection.CARS_PER_STEP`. **Static nested class:** `TrafficSubsystem.WaitStats` (private static; carsPassed, totalWaitSteps). **Anonymous inner class:** `new WindowAdapter() { ... }` in `CityDashboard`. Lambdas are the short form of anonymous classes. | **No static init block, no instance init block**, so no initialization-order puzzle. **No non-static (inner) member class.** Revise: "a static block runs once when the class loads; an instance block runs before the constructor body". |
| 4 | Inheritance | **Multilevel:** `Vehicle -> Car -> ElectricCar`. **Hierarchical:** `Car`, `Bike`, `Truck`, `Ambulance` all extend `Vehicle`; `CityException` has four children. **Single:** `AbstractSubsystem extends Thread`, `CityDashboard extends JFrame`. **Multiple via interfaces:** `AbstractSubsystem implements Subsystem, CityEventListener`; `ElectricCar extends Car implements Chargeable`. **Constructor order:** `new ElectricCar()` runs the `Vehicle` constructor, then `Car`'s protected constructor, then the `ElectricCar` body (`super(VehicleType.EV, "E-")`, then `battery = ...`). **`super.method()`:** `ElectricCar.getPriority()` returns `battery < 20 ? 2 : super.getPriority()`. **`final` method:** `AbstractSubsystem.run()`. | `super.field` is not used, and no one tries to extend a `final` class (`CityController`, `CityEvent`, `VehicleFactory` are final). |
| 5 | Polymorphism | **Overriding with `@Override`:** every `getPriority()`, `Ambulance.isEmergency()`, every `onEvent`, `statusReport`, `toString`. **Upcasting:** the factory returns `Vehicle`; a `Subsystem` variable holds a `TrafficSubsystem`; a `SignalStrategy` variable holds a `FixedTimerStrategy`. **Downcasting:** `(Ambulance) e.getPayload()`, `(Vehicle) e.getPayload()`, `((Chargeable) v).isFullyCharged()` in `ParkingLot.wantsToLeave`, `((Thread) s).join(1000)` in `CityController.shutdown`. **Reference vs object type:** `v instanceof Ambulance` in `TrafficSubsystem.tick`. **Overloading:** `EventBus.publish`. | **No static method hiding** and **no variable (field) hiding** puzzle. Rule to remember: *instance methods* are chosen by the object's real type at run time; *fields* and *static methods* by the reference type at compile time. |
| 6 | Abstract classes and interfaces | **Abstract classes:** `Vehicle` (abstract `getPriority`), `AbstractSubsystem` (abstract `tick`). **Interfaces:** `Subsystem`, `CityEventListener`, `SignalStrategy`, `PricingStrategy`, `Chargeable`. **Default method:** `Chargeable.isFullyCharged()` (built on the abstract `getBatteryPercent()`), called from `ParkingLot`. **Both combined:** `AbstractSubsystem` is an abstract class that implements two interfaces and leaves `onEvent` and `statusReport` abstract. | **No interface extending another interface.** **No static or private interface methods.** No "two defaults with the same name" (diamond) example. |
| 7 | Arrays and collections | **ArrayList:** most lists. **LinkedList as a queue with `offer` / `peek` / `poll`:** `Intersection.arrive` (`offer`) and `step` (`peek`, `poll`), `EventBus.publish` (`offer`) and `run` (`poll`), `RepairCrew.reportFault` / `run`. **Explicit `Iterator` with `hasNext` / `next` / `remove`, plus `Map.Entry.setValue`:** `LightingSubsystem.countDownAmbulanceTicks`. **Generics:** `ArrayList<Vehicle>`, `HashMap<Direction, LinkedList<Vehicle>>`, `Comparable<Vehicle>`, `Iterator<Map.Entry<String, Integer>>`. HashMap, HashSet, PriorityQueue are also used. | **No `ListIterator`.** **Plain arrays are barely used** (only `String[] AREAS`, `SlotType[]`, `int[]` in `getAmbulancesOnRoad`). No 2D array, no `Arrays` / `Collections` utility methods, no writing your own generic class or method. |
| 8 | Exception handling | **Checked custom exceptions:** `CityException` and 4 children. **`throw` / `throws`:** `ParkingLot.park`, `Intersection.arrive`, `ParkingSlot.occupy`. **Multi-catch:** `ParkingSubsystem.findParking`. **Catch by parent type:** `catch (CityException e)`. **Propagation / re-throw as another type:** `ParkingSlot.occupy` throws, `ParkingLot.park` catches and throws `IllegalStateException`. **`finally` together with `return`:** `RepairCrew.run`: `catch (InterruptedException e) { return; } finally { currentJob = null; }` (finally still runs). **Unchecked:** `IllegalStateException`, and the `RuntimeException` safety catches in `AbstractSubsystem.run` and `EventBus.deliver`. **InterruptedException** in every thread loop. | **No nested try-catch puzzle**, **no exception thrown inside a catch or a finally**, **no `return` inside `finally` overriding a return value**, no try-with-resources. Revise: "a `return` in `finally` replaces an earlier return value". |
| 9 | Multithreading | **`extends Thread`:** `AbstractSubsystem`. **`implements Runnable`:** `SimulationClock`, `EventBus`, `RepairCrew` (`new Thread(this, "SimClock")`). **`start()` vs `run()`:** `CityController.start` calls `start()`; `run()` holds the loop (calling `run()` directly would run the loop on the caller's thread and never return). **Thread names:** `super(name)` in `AbstractSubsystem`; `Thread.currentThread().getName()` in the tick error message. **`sleep`:** every loop. **`join(timeout)`:** `CityController.shutdown` (`((Thread) s).join(1000)`). **Priority:** `traffic.setPriority(Thread.NORM_PRIORITY + 2)` in `CityController.start`. Also `wait` / `notify`, `synchronized`, `volatile`, daemon threads, `interrupt`. | **Thread states are not shown** (`getState()`: NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED). **No plain `join()` without a timeout.** **No single Runnable shared by two Thread objects** (each Runnable here feeds one thread). No `ExecutorService`. Revise these from your notes. |

Tip for the viva: for a topic marked "not shown", say "that is not in my project, but here is how it works" and explain from your notes. For a topic that is shown, point to your own file as an example.

### 2-minute UML talk (project report slot)

You have 2 minutes, so talk in this order (about 20-30 seconds per item):

1. **Class diagram, big boxes first (20 s):** `CityController` (Singleton) owns the `EventBus`, the `SimulationClock` and three subsystems. "Subsystems never call each other; they only use the bus."
2. **Vehicle hierarchy (25 s):** `Vehicle` (abstract, implements `Comparable`) with children `Car`, `Bike`, `Truck`, `Ambulance`; `ElectricCar` extends `Car` (multilevel) and implements the `Chargeable` interface. Mention the abstract `getPriority()`.
3. **AbstractSubsystem hierarchy (25 s):** `AbstractSubsystem` extends `Thread` and implements `Subsystem` and `CityEventListener`; `TrafficSubsystem`, `LightingSubsystem`, `ParkingSubsystem` extend it (Template Method: `run()` calls abstract `tick()`).
4. **Interfaces and strategy (20 s):** `SignalStrategy` with `FixedTimerStrategy` and `AdaptiveStrategy`, used by `Intersection`; `PricingStrategy` with `DynamicPricing`, used by `ParkingLot`. Custom exceptions under `CityException`.
5. **Sequence diagram: ambulance dispatch (30 s):** GUI button -> `CityController.dispatchAmbulance()` -> `TrafficSubsystem.dispatchAmbulance()` (preempts INT-1 and INT-2, publishes `AMBULANCE_DISPATCHED`) -> `EventBus` -> `LightingSubsystem.onEvent` (area FULL). Then `leaveJunction` publishes `AMBULANCE_MOVED` (lights again) and finally `AMBULANCE_ARRIVED` -> `ParkingSubsystem.parkAmbulance` -> `AMBULANCE_PARKED`.

Finish with: "the three scenarios (lot full, ambulance, accident) all use the same bus".

---

## 2. The big picture

### City layout (built in `core/CityController.java`, method `buildCity`)

```
   WEST area               CENTRAL area             EAST area
   INT-1 ----------------- INT-2 ----------------- INT-3  --> City Hospital
   Mall Lot (10 slots)     (no parking lot)         Hospital Lot (8 slots)
   lights SL-W1,W2,W3      lights SL-C1,C2,C3       lights SL-E1,E2,E3
```

* 3 areas: `CityController.AREAS = {"West", "Central", "East"}`.
* 3 intersections: INT-1 (West), INT-2 (Central), INT-3 (East). One main east-west road, so the city is a straight line, not a grid.
* 9 street lights: ids built as `"SL-" + area.charAt(0) + i`, so `SL-W1 ... SL-E3`.
* 2 parking lots (both share one `DynamicPricing` object):

| Lot | Area | Slots | Total |
|---|---|---|---|
| Mall Lot | West | 3 BIKE, 4 STANDARD, 2 EV_CHARGING, 1 HEAVY | 10 |
| Hospital Lot | East | 2 BIKE, 3 STANDARD, 1 EV_CHARGING, 1 HEAVY, 1 EMERGENCY | 8 |

### The architecture in one picture

```
                    GUI (CityDashboard + CityMapPanel)
                      |  buttons call            ^  reads only (getters)
                      v                          |
                +-----------------------------------+
                |   CityController  (Singleton)     |  builds the city,
                |   dispatchAmbulance / reportAccident / clearAccident
                |   toggleSignalMode / statusText / endOfDayReport
                +-----------------------------------+
                      | publish()                  ^ subscribe()
                      v                            |
                +-----------------------------------+
                |   EventBus   (queue + own thread) |   Observer / publish-subscribe
                +-----------------------------------+
                  |  onEvent()     |  onEvent()     |  onEvent()
                  v                v                v
          +--------------+  +---------------+  +----------------+
          | Traffic      |  | Lighting      |  | Parking        |
          | Subsystem    |  | Subsystem     |  | Subsystem      |
          | 3 x          |  | 9 x StreetLight|  | 2 x ParkingLot |
          | Intersection |  | + RepairCrew  |  |                |
          +--------------+  +---------------+  +----------------+
                  ^                 ^                  ^
                  +----- all three read the same SimulationClock -----+
```

Key idea: **subsystems never call each other.** They only `publish` events and `subscribe` to events. For example, Traffic does not know Parking exists; it publishes `CAR_NEEDS_PARKING` and Parking happens to listen for it.

### Who listens to what (from the constructors)

| Subsystem | Subscribes to (`bus.subscribe(this, ...)`) |
|---|---|
| `TrafficSubsystem` | `ACCIDENT_REPORTED`, `ROAD_REOPENED` |
| `LightingSubsystem` | `VEHICLES_PASSING`, `ACCIDENT_REPORTED`, `ROAD_REOPENED`, `AMBULANCE_DISPATCHED`, `AMBULANCE_MOVED` |
| `ParkingSubsystem` | `CAR_NEEDS_PARKING`, `AMBULANCE_ARRIVED`, `ACCIDENT_REPORTED`, `ROAD_REOPENED` |

All other event types (`CAR_REROUTED`, `PARKING_QUEUE_SPILLOVER`, `AMBULANCE_PARKED`, `LIGHT_FAULT`, `LIGHT_REPAIRED`, `SIGNAL_MODE_CHANGED`, `NIGHT_STARTED`, `DAY_STARTED`) have **no subscribers**. They exist only so they appear in the event log (and in the console echo). Lighting finds out about night by asking the clock (`clock.isNight()`), not by listening to `NIGHT_STARTED`.

### Event types and what they carry (`events/EventType.java`)

`CityEvent` has: `type`, `source`, `area`, `payload` (an `Object`), `time`. The payload type depends on the event:

| EventType | Published by | Payload |
|---|---|---|
| `VEHICLES_PASSING` | `TrafficSubsystem.tick` | `Integer` (how many cars crossed) |
| `CAR_NEEDS_PARKING` | `TrafficSubsystem.tick` | the `Vehicle` |
| `CAR_REROUTED`, `PARKING_QUEUE_SPILLOVER` | `ParkingSubsystem` | `String` message |
| `AMBULANCE_DISPATCHED`, `AMBULANCE_MOVED` | `TrafficSubsystem` | `String` message |
| `AMBULANCE_ARRIVED` | `TrafficSubsystem.leaveJunction` | the `Ambulance` object |
| `AMBULANCE_PARKED` | `ParkingSubsystem.parkAmbulance` | `String` message |
| `ACCIDENT_REPORTED`, `ROAD_REOPENED` | `CityController` | `String` message |
| `LIGHT_FAULT` | `LightingSubsystem.tick` | `String` message |
| `LIGHT_REPAIRED` | `RepairCrew.run` | light id `String` |
| `SIGNAL_MODE_CHANGED` | `CityController.toggleSignalMode` | `String` message |
| `NIGHT_STARTED`, `DAY_STARTED` | `SimulationClock.run` | `String` message |

The enum has a boolean "logged" flag. `VEHICLES_PASSING` and `CAR_NEEDS_PARKING` are `false` (too frequent), so they are not shown in the log. All others are `true`.

### Every thread in the program

| Thread name | Class | What it does | Tick / wait |
|---|---|---|---|
| `SimClock` | `SimulationClock implements Runnable` (creates `new Thread(this, "SimClock")`) | Moves the city time forward 10 min per second; publishes `NIGHT_STARTED` / `DAY_STARTED` when night changes. | `Thread.sleep(1000)` |
| `EventBus` | `EventBus implements Runnable` | Consumer: waits for events, delivers each to subscribers. | `wait()` until `notify()` |
| `Traffic` | `TrafficSubsystem extends AbstractSubsystem extends Thread` | Cars arrive, signals step, cars pass, ambulances drive. | every **500 ms** |
| `Lighting` | `LightingSubsystem extends AbstractSubsystem` | Decides the mode of each light, counts energy, reports faults. | every **1000 ms** |
| `Parking` | `ParkingSubsystem extends AbstractSubsystem` | Ticks each lot (cars leave and pay, EVs charge, queue admitted), spillover warnings. | every **1000 ms** |
| `RepairCrew` | `RepairCrew implements Runnable` | Consumer: waits for broken lights, then repairs one at a time. | `wait()`, then `Thread.sleep(4000)` per repair |
| `AWT-EventQueue-0` (Swing thread) | built into Swing | Draws the GUI; the `javax.swing.Timer` refreshes every **200 ms**. | 200 ms |
| `main` | `UrbanFlowApp.main` | Builds the controller, starts threads. In GUI mode it ends quickly. In console mode it sleeps 1 s at a time and fires the script. | 1000 ms (console) |

All worker threads are **daemon threads** (`setDaemon(true)`), meaning they do not keep the program alive by themselves. In GUI mode the Swing thread keeps it alive; closing the window calls `System.exit(0)`.

Two ways to make a thread appear in the code (both are in the syllabus):
* `extends Thread`: `AbstractSubsystem` (so Traffic, Lighting, Parking are themselves threads).
* `implements Runnable`: `SimulationClock`, `EventBus`, `RepairCrew` (they create a `Thread` around themselves).

`CityController.start()` starts them in order: `bus.start()`, `clock.start()`, then `traffic.setPriority(Thread.NORM_PRIORITY + 2)` (thread priority: signals are the most time-critical; only a hint to the OS), then each `Subsystem.start()`. `LightingSubsystem.start()` is overridden to start `crew` first and then call `super.start()`.

---

## 3. Follow one tick

All three subsystems share the same loop, written once in `core/AbstractSubsystem.java`, method `run()` (marked `final`):

```java
while (running) {
    try { tick(); } catch (RuntimeException ex) { ... print and carry on ... }
    try { Thread.sleep(tickMillis); } catch (InterruptedException e) { return; }
}
```

This is the **Template Method** pattern: the base class fixes the skeleton, subclasses fill in `tick()`.

### 3.1 One TrafficSubsystem tick (every 500 ms)

`traffic/TrafficSubsystem.java`, method `tick()`:

1. **Pick the arrival rate:** `rate = clock.isPeakHour() ? 1.5 : (clock.isNight() ? 0.6 : 1.0)`. (Rush hour is checked first, so 19:00-19:59 counts as rush hour even though it is night.)
2. For **each intersection**, in order INT-1, INT-2, INT-3:
   1. **Arrivals.** `arrivals = (int) rate`, plus one more with probability equal to the fractional part (for 1.5: 1 car, or 2 cars half the time). For each, `VehicleFactory.randomVehicle()` makes a car and `randomDirection()` picks the road: WEST 35%, EAST 35%, NORTH 15%, SOUTH 15%. It calls `in.arrive(vehicle, direction)`. If the junction is blocked this throws `RoadBlockedException`; the catch calls `recordDiverted()`.
   2. **Signal step.** `in.step()` returns the list of vehicles that crossed during this step (see below).
   3. **For each vehicle that crossed:**
      * if it is an `Ambulance` (`instanceof`), remember it in `leavingJunction`;
      * otherwise `recordPassed(in.waitedSteps(v))` for the statistics, and with 8% chance (`PARKING_CHANCE = 0.08`) publish `CAR_NEEDS_PARKING` with the vehicle as payload.
   4. If anything crossed, publish `VEHICLES_PASSING` with the count (the street lights use this as a "motion sensor").
3. `driveAmbulances()`: every ambulance on the road loses one travel tick (`a.drive()`); those that arrive join the queue of their junction (road direction WEST).
4. For ambulances that just crossed a junction, `leaveJunction(a)` sends them to the next junction or to the hospital.

**What `Intersection.step()` does** (`traffic/Intersection.java`):

```java
stepCount++;
if (emergencyRequests > 0) {
    green = emergencyDirection;                 // ambulance coming: force green its way
} else if (blocked) {
    return passed;                              // accident: all red, nobody passes
} else if (greenRemaining <= 0
        || (strategy.endsGreenEarly() && queues.get(green).isEmpty())) {
    green = strategy.chooseNextGreen(green, lengths);
    greenRemaining = strategy.greenTimeFor(green, lengths);
}
greenRemaining--;
// then up to CARS_PER_STEP (= 2) vehicles leave the queue of the green road
```

Order of priority inside `step()`: ambulance green wave, then blocked, then normal signal logic.

**Fixed vs Adaptive** (both implement `SignalStrategy`):

| | `FixedTimerStrategy(10)` | `AdaptiveStrategy(3, 15, 30)` |
|---|---|---|
| Next green | `current.next()`: NORTH, EAST, SOUTH, WEST in a loop | the road with the **longest queue** (starts from `current.next()`, so ties go to the next road in order) |
| Green length | always 10 steps | `round(30 * myQueue / totalQueued)`, clamped between 3 and 15; if nothing is waiting, 3 |
| Ends early when road is empty? | no (`endsGreenEarly()` false) | yes (true) |

`TrafficSubsystem` creates both: `fixed = new FixedTimerStrategy(10)`, `adaptive = new AdaptiveStrategy(3, 15, 30)`, and starts in adaptive.

One "step" is one traffic tick = 0.5 real seconds. So "avg wait ... s" in the reports is really measured in steps (see section 10).

### 3.2 One LightingSubsystem tick (every 1000 ms)

`lighting/LightingSubsystem.java`, method `tick()`. For each of the 9 lights, in this **order of importance**:

| Condition | Wanted mode |
|---|---|
| area is in `accidentAreas` | `FLASHING` (brightness 60) |
| area is in `ambulanceTicks` | `FULL` (100) |
| daytime | `OFF` (0) |
| night | `DIM` (30), and with probability `cars * 0.12` (cars passing in that area since last tick) `light.vehicleNearby()` is called, which makes the light FULL for 3 ticks |

Then:
1. `light.applyMode(wanted)`. A `FAULTY` light ignores it. A DIM request becomes FULL while `brightTicksLeft > 0`.
2. `light.tick(hoursPerTick, night)`:
   * at night adds `150 W * brightness% * hours` to `energyWh` and `150 W * hours` to `baselineWh` (the old always-full light), where `hours = 10/60` per tick;
   * counts down the brightness timer;
   * if the light is not OFF/FAULTY, a 0.2% chance (`FAILURE_CHANCE = 0.002`) it becomes `FAULTY`, and `tick` returns `true`.
3. Broken lights are collected in a list and, **after leaving the synchronized block**, each one is handed to `crew.reportFault(light)` and `LIGHT_FAULT` is published.
4. `countDownAmbulanceTicks()`: each area's ambulance counter drops by 1; at 0 the area is removed (so FULL lasts about 6 ticks = 6 s after each `AMBULANCE_DISPATCHED` / `AMBULANCE_MOVED`).
5. `carsPassing.clear()`.

Events arrive separately on the bus thread (`onEvent`, which is `synchronized`) and only change the three helper collections: `carsPassing`, `accidentAreas`, `ambulanceTicks`. The tick thread reads them. That is why both `onEvent` and the main part of `tick` use the same lock.

The crew then repairs one light every 4 seconds (section 7).

### 3.3 One ParkingSubsystem tick (every 1000 ms)

`parking/ParkingSubsystem.java`, method `tick()`. For each lot:

1. `lot.tick(peak, clock.getSimMinutesPerSecond())` (`parking/ParkingLot.java`), which is `synchronized`:
   * `occupancy = getOccupancy()` is read once at the start (used for the price);
   * for each slot: `s.tick()` adds 1 to `ticksParked`; an EV in an `EV_CHARGING` slot gets `charge(10)`; `wantsToLeave` decides if it leaves. EVs in an `EV_CHARGING` slot: leave only when `((Chargeable) v).isFullyCharged()` (battery 100%, a default method), then with a 30% chance per tick. Other vehicles: parked at least 6 ticks, then a 4% chance each tick. Ambulances: leave after `AMBULANCE_STAY_TICKS = 8`;
   * when someone leaves: `revenue += pricePerHour * hours`, `hours = ticksParked * 10 / 60`, then `s.vacate()`;
   * `admitWaitingVehicles()`: take vehicles from the priority queue one by one (highest priority first); if a slot fits and the lot is open (or the vehicle is an ambulance) park it, else put it back.
2. **Spillover check:** if `lot.queueLength() >= 4` (`SPILLOVER_LIMIT`) and this is the first time (`markSpillover` returns true), publish `PARKING_QUEUE_SPILLOVER`. When the queue drops below 4 the flag is reset, so it can warn again later.

Most of the parking logic is not in `tick()`. It happens in `onEvent`, when a car asks for parking (section 4.4).

**Prices** (`parking/DynamicPricing.java`): base per hour: BIKE 10, STANDARD 30, EV_CHARGING 50, HEAVY 80, EMERGENCY 0 (free). Multiplier: occupancy over 90% gives x2.0, over 70% gives x1.5, otherwise x1.0; during rush hour **add** 0.25 to the multiplier.

---

## 4. The three scenarios, traced step by step

Remember: `EventBus` calls `onEvent()` on **its own thread**, not on the sender's thread.

### 4.1 Scenario 1: Lot full, car sent to the other lot

| Step | Where | What happens |
|---|---|---|
| 1 | `TrafficSubsystem.tick` | A car crosses a junction. With 8% chance (`PARKING_CHANCE = 0.08`): `bus.publish(CAR_NEEDS_PARKING, in.getId(), in.getArea(), v)`. |
| 2 | `EventBus.deliver` | Calls `ParkingSubsystem.onEvent`. |
| 3 | `ParkingSubsystem.onEvent` | `case CAR_NEEDS_PARKING: findParking((Vehicle) e.getPayload(), e.getArea());` |
| 4 | `findParking` | `preferred = preferredLot(v, area)`: the lot in the car's area (West gives Mall Lot, East gives Hospital Lot). Central has no lot, so it picks the lot with **strictly more** free slots for that vehicle type (a tie gives Mall Lot, the first in the list). |
| 5 | `ParkingLot.park` | If no free slot of an allowed type: `throw new ParkingFullException(name, v.getId())`. If the lot is closed and the vehicle is not an ambulance: `throw new LotClosedException(name)`. |
| 6 | `findParking` catch | `catch (ParkingFullException \| LotClosedException e) { reason = e.getMessage(); }` |
| 7 | `findParking` loop | Tries every other lot with `other.park(v)`. If it works: `addReroute()` and `bus.publish(CAR_REROUTED, preferred.getName(), area, v.getId() + " sent to " + other.getName() + " (" + reason + ")")`. |
| 8 | log | `CAR_REROUTED` has no subscribers; it shows in the dashboard event log and `reroutes` count in the report. |

If **every** lot refuses, see 4.4 (queue or give up).

### 4.2 Scenario 2: Ambulance

| Step | Where | What happens |
|---|---|---|
| 1 | GUI button | `ambulanceButton.addActionListener(e -> city.dispatchAmbulance())` |
| 2 | `CityController.dispatchAmbulance` | `traffic.dispatchAmbulance(new Ambulance())` |
| 3 | `TrafficSubsystem.dispatchAmbulance` | **Green wave:** `intersections.get(0).preempt(Direction.WEST)` and the same for index 1 (the next two junctions are told right away). `a.startDriving(0, 4)` = 4 ticks (2 s) to reach INT-1. Added to `ambulancesOnRoad`; `ambulanceTrips++`. Publishes `AMBULANCE_DISPATCHED` with area "West". |
| 4 | `LightingSubsystem.onEvent` | `ambulanceTicks.put("West", 6)`: West lights become FULL for about 6 s. |
| 5 | `driveAmbulances` (each traffic tick) | `a.drive()` reduces `travelTicksLeft`; at 0 it joins INT-1's queue: `arrive(a, Direction.WEST)` (never throws for emergency vehicles). |
| 6 | `Intersection.step` | `emergencyRequests > 0`, so `green = emergencyDirection` (WEST). Cars are removed from the WEST queue 2 per step. The ambulance goes at the **end** of that queue (it is added with `offer`, which adds at the back), so cars already waiting ahead of it clear first. When it crosses, `emergencyRequests--` releases the hold. |
| 7 | `TrafficSubsystem.leaveJunction` | Next junction exists: `intersections.get(next + 1).preempt(WEST)` (keeps the wave two junctions ahead), `startDriving(next, 4)`, publish `AMBULANCE_MOVED` for the target junction's area (Central, later East). Lighting makes that area FULL for 6 s. |
| 8 | after INT-3 | `publish(AMBULANCE_ARRIVED, last.getId(), last.getArea(), a)` with the `Ambulance` as payload. |
| 9 | `ParkingSubsystem.onEvent` | `parkAmbulance((Ambulance) e.getPayload())`. |
| 10 | `parkAmbulance` | Finds the lot that `hasSlotType(SlotType.EMERGENCY)` (Hospital Lot), calls `lot.park(a)`. `SlotType.allowedFor(AMBULANCE)` is `{EMERGENCY, STANDARD}`, so it takes the reserved bay first. Records the response time (`recordAmbulance`) and publishes `AMBULANCE_PARKED` with the text "parked" (it got a slot, the bay or a standard slot) or "first in queue" (no slot free at all; priority 10 puts it at the front). |
| 11 | `ParkingLot.tick` | After 8 ticks (`AMBULANCE_STAY_TICKS`) the ambulance leaves and the bay is free again. |

Which junction gets which hold: INT-1 and INT-2 at dispatch, INT-3 when the ambulance leaves INT-1. When leaving INT-2, `next + 1 = 3` is not less than 3, so nothing more is held. Each junction gets exactly one hold per ambulance.

Why ambulances are never blocked: `Intersection.arrive` only throws if `blocked && !v.isEmergency()`; `ParkingLot.park` only throws `LotClosedException` if `closed && !v.isEmergency()`; and `Intersection.step` checks `emergencyRequests > 0` **before** it checks `blocked`.

### 4.3 Scenario 3: Accident

| Step | Where | What happens |
|---|---|---|
| 1 | GUI | `accidentButton.addActionListener(e -> city.reportAccident((String) areaBox.getSelectedItem()))` |
| 2 | `CityController.reportAccident(area)` | `bus.publish(EventType.ACCIDENT_REPORTED, "Controller", area, "road blocked")`. One event, three listeners. |
| 3a | `TrafficSubsystem.onEvent` | `setBlocked(area, true)`: every intersection whose area matches gets `setBlocked(true)`. From then on `Intersection.arrive` throws `RoadBlockedException(id)` for normal vehicles; the catch in `TrafficSubsystem.tick` calls `recordDiverted()` ("cars diverted"). `step()` returns an empty list, so the junction is all red. |
| 3b | `LightingSubsystem.onEvent` | `accidentAreas.add(area)`. In the next `tick` every light of that area wants `FLASHING` (the first rule, so it beats ambulance FULL and night DIM). |
| 3c | `ParkingSubsystem.onEvent` | `setLotsClosed(area, true)`. Any lot in that area becomes closed. `ParkingLot.park` now throws `LotClosedException` for non-ambulances, and `admitWaitingVehicles` stops letting waiting cars in. Cars that wanted that lot are rerouted (as in 4.1; the reason text says "... is closed (accident in the area)"). |
| 4 | clearing | `clearAccident(area)` publishes `ROAD_REOPENED`; each subsystem undoes its part (`setBlocked(false)`, `accidentAreas.remove`, `setClosed(false)`). |

Remember: **Central has no lot**, so a Central accident blocks INT-2 and flashes the lights but closes nothing. To show `LotClosedException` choose West or East.

### 4.4 Normal parking flow in detail (`findParking`)

```
CAR_NEEDS_PARKING
      |
      v
preferredLot(v, area)  --- lot in same area, else the one with most free slots for this vehicle type
      |
      v
preferred.park(v) ---------- success --------------------------> done (parked)
      |
      | ParkingFullException or LotClosedException (multi-catch, remember reason)
      v
for each OTHER lot: other.park(v)
      | success -> addReroute(); publish CAR_REROUTED -----------> done
      | CityException -> try the next lot
      v
all lots refused
      |
queueAt = preferred.isClosed() ? firstOpenLot() : preferred
      |
      +-- queueAt == null  OR  queueAt.queueLength() >= 8 (MAX_QUEUE) --> addGaveUp()  (driver leaves)
      |
      +-- otherwise  queueAt.addToWaiting(v)   (PriorityQueue)
                        |
                        v
                each parking tick: admitWaitingVehicles()
                        highest Vehicle.getPriority() first, then lowest id number
```

Which slot types can each vehicle use (`SlotType.allowedFor`, in order of preference):

| Vehicle | Allowed slots |
|---|---|
| BIKE | BIKE, STANDARD |
| CAR | STANDARD |
| EV | EV_CHARGING, STANDARD |
| TRUCK | HEAVY |
| AMBULANCE | EMERGENCY, STANDARD |

Priorities (`getPriority`): Truck 0, Car 1, Bike 1, ElectricCar 2 if battery under 20% else 1, Ambulance 10. Higher priority leaves the waiting queue first; same priority means first come, first served (lower creation number).

Vehicle mix from `VehicleFactory.randomVehicle`: Car 50%, Bike 25%, ElectricCar 17%, Truck 8%.

---

## 5. Class-by-class table (all 41 files)

Paths are inside `src/com/urbanflow/`.

### Root and `core`

| File | Kind | Purpose | Main OOP concept |
|---|---|---|---|
| `UrbanFlowApp.java` | class | Entry point `main`. Starts GUI or the scripted console demo. | static method, exceptions (`throws InterruptedException`) |
| `core/CityController.java` | final class | Builds the city, starts threads, offers operator actions and reports. The only object the GUI talks to. | Singleton, static, encapsulation, polymorphism (loops over `Subsystem`, downcast + `join(1000)`), thread priority |
| `core/Subsystem.java` | interface | Contract: `getName`, `start`, `shutdown`, `statusReport`. | Interface, abstraction |
| `core/AbstractSubsystem.java` | abstract class | Thread + fixed loop; subclasses write `tick()`. | Abstract class, inheritance, Template Method, `extends Thread`, `final` method, volatile |
| `core/SimulationClock.java` | class | Accelerated clock; knows night and rush hour. | `implements Runnable`, synchronized, encapsulation |

### `events`

| File | Kind | Purpose | Main OOP concept |
|---|---|---|---|
| `events/CityEvent.java` | final class | One immutable message (type, source, area, payload, time). | Encapsulation, immutability |
| `events/CityEventListener.java` | interface | `onEvent(CityEvent)`. | Interface, Observer pattern |
| `events/EventBus.java` | class | Queue + dispatcher thread; `subscribe` and `publish`. | Observer, producer-consumer (wait/notify), LinkedList as queue (`offer`/`poll`) |
| `events/EventType.java` | enum | 15 event kinds, each with a "logged" flag. | Enum with field and constructor |

### `exceptions`

| File | Kind | Purpose | Main OOP concept |
|---|---|---|---|
| `exceptions/CityException.java` | class | Base of all UrbanFlow errors; extends `Exception` (checked). | Custom exception, inheritance |
| `exceptions/RoadBlockedException.java` | class | Car tried to enter a blocked junction. | Custom checked exception |
| `exceptions/ParkingFullException.java` | class | No free slot of a fitting type. | Custom checked exception |
| `exceptions/LotClosedException.java` | class | Lot closed by an accident. | Custom checked exception |
| `exceptions/SlotAlreadyTakenException.java` | class | Safety check: slot already occupied. | Custom checked exception, defensive programming |

### `model` (vehicles)

| File | Kind | Purpose | Main OOP concept |
|---|---|---|---|
| `model/Vehicle.java` | abstract class | Base of all vehicles: id, type, priority, ordering. | Abstraction, `Comparable`, static counter |
| `model/Car.java` | class | Normal car, priority 1, id `C-n`. Parent of `ElectricCar`. | Constructor chaining `this(...)`, protected constructor, overriding |
| `model/Bike.java` | class | Bike, priority 1, id `B-n`. | Inheritance, overriding |
| `model/Truck.java` | class | Truck, priority 0, id `T-n`. | Inheritance, overriding |
| `model/ElectricCar.java` | class | EV with a battery; priority 2 if battery under 20%, else `super.getPriority()`. | Multilevel inheritance (Vehicle -> Car -> ElectricCar) + interface, `super.method()`, synchronized |
| `model/Ambulance.java` | class | Emergency vehicle, priority 10, tracks its route position. | Inheritance, overriding `isEmergency` |
| `model/Chargeable.java` | interface | `getBatteryPercent`, `charge`, default method `isFullyCharged()`. | Interface, default method, polymorphism (`instanceof` + downcast) |
| `model/VehicleType.java` | enum | BIKE, CAR, EV, TRUCK, AMBULANCE. | Enum |
| `model/VehicleFactory.java` | final class | `randomVehicle()` with weighted odds. | Factory pattern, static, private constructor |

### `traffic`

| File | Kind | Purpose | Main OOP concept |
|---|---|---|---|
| `traffic/TrafficSubsystem.java` | class | Traffic thread: arrivals, signals, ambulance driving, statistics (per signal mode in `WaitStats`). | Inheritance, Observer (subscribes), exceptions, synchronized, static nested class |
| `traffic/Intersection.java` | class | One junction with 4 FIFO queues; `arrive`, `step`, `preempt`. | Encapsulation, synchronized, LinkedList as queue (`offer`/`peek`/`poll`), HashMap, Strategy user |
| `traffic/SignalStrategy.java` | interface | How a signal picks next green and its length. | Strategy pattern, interface |
| `traffic/FixedTimerStrategy.java` | class | Same green time, fixed order. | Interface implementation, polymorphism |
| `traffic/AdaptiveStrategy.java` | class | Longest queue first; green time by share of queue. | Interface implementation, polymorphism |
| `traffic/Direction.java` | enum | NORTH, EAST, SOUTH, WEST with `next()`. | Enum with method |

### `lighting`

| File | Kind | Purpose | Main OOP concept |
|---|---|---|---|
| `lighting/LightingSubsystem.java` | class | Chooses each light's mode, energy accounting, reports faults. | Inheritance, synchronized, HashSet/HashMap, explicit `Iterator` |
| `lighting/StreetLight.java` | class | One pole: mode, energy, random failure. | Encapsulation (private fields, guarded methods), synchronized |
| `lighting/LightMode.java` | enum | OFF 0, DIM 30, FULL 100, FLASHING 60, FAULTY 0 (brightness %). | Enum with field |
| `lighting/RepairCrew.java` | class | Thread that repairs faulty lights one by one. | `implements Runnable`, wait/notify, producer-consumer, `finally` |

### `parking`

| File | Kind | Purpose | Main OOP concept |
|---|---|---|---|
| `parking/ParkingSubsystem.java` | class | Chooses lots, rerouting, queue/give up, ambulance bay. | Inheritance, custom exceptions (multi-catch), Observer |
| `parking/ParkingLot.java` | class | Typed slots, priority waiting queue, pricing, open/closed. | Encapsulation, PriorityQueue, `throws`, synchronized, Strategy user |
| `parking/ParkingSlot.java` | class | One space; throws if already occupied. | Encapsulation, custom exception |
| `parking/SlotType.java` | enum | BIKE, STANDARD, EV_CHARGING, HEAVY, EMERGENCY + `allowedFor`. | Enum with static method |
| `parking/PricingStrategy.java` | interface | `pricePerHour(type, occupancy, peak)`. | Strategy pattern, interface |
| `parking/DynamicPricing.java` | class | Price grows with occupancy and rush hour. | Interface implementation |

### `gui`

| File | Kind | Purpose | Main OOP concept |
|---|---|---|---|
| `gui/CityDashboard.java` | class | Main window: map, status, buttons, log, refresh timer. | Swing, inheritance (`extends JFrame`), anonymous inner class, lambdas |
| `gui/CityMapPanel.java` | class | Draws the city with Graphics2D. Read-only. | Swing, inheritance (`extends JPanel`), overriding `paintComponent` |

Count check: 5 + 4 + 5 + 9 + 6 + 4 + 6 + 2 = 41 files.

---

## 6. OOP concept index

### Encapsulation (private data, controlled access)

* `lighting/StreetLight.java`: `mode`, `energyWh`, `baselineWh`, `brightTicksLeft` are private. The rule "a FAULTY light stays faulty until repaired" lives in one method:
  ```java
  public synchronized void applyMode(LightMode wanted) {
      if (mode == LightMode.FAULTY) { return; }
      ...
  ```
* `traffic/Intersection.java`: queues, `blocked`, `emergencyRequests` are private; outsiders can only call `arrive`, `step`, `preempt`, `setBlocked`.
* `parking/ParkingLot.java` and `ParkingSlot.java`: the slot list is private; `ParkingSlot.occupy` refuses double use.
* `events/CityEvent.java`: all fields `private final`, getters only, no setters (immutable).
* Getters that return **copies**: `Intersection.getQueueSnapshot`, `getQueueLengths`, `ParkingLot.getSlotTypes`, `getOccupantTypes`, `EventBus.getRecentEvents`.

### Inheritance

* `model/Vehicle` (abstract) is extended by `Car`, `Bike`, `Truck`, `Ambulance` (hierarchical). `ElectricCar` extends `Car`, so the chain is **Vehicle -> Car -> ElectricCar** (multilevel).
* Constructor chaining: `Car()` calls `this(VehicleType.CAR, "C-")`, which is the protected `Car(VehicleType, String)`, which calls `super(type, idPrefix)` (Vehicle). `ElectricCar()` calls `super(VehicleType.EV, "E-")` (the protected Car constructor). Order for `new ElectricCar()`: Vehicle body, Car body, ElectricCar body.
* `ElectricCar.getPriority()` is `battery < 20 ? 2 : super.getPriority()`, reusing Car's value 1.
* `core/AbstractSubsystem` extends `Thread`; `TrafficSubsystem`, `LightingSubsystem`, `ParkingSubsystem` extend it.
* `exceptions/CityException` extends `Exception`; four exceptions extend `CityException`.
* `gui/CityDashboard extends JFrame`, `CityMapPanel extends JPanel`.
* `super(...)` calls: `Car()` calls `super(VehicleType.CAR, "C-")`; `AbstractSubsystem` calls `super(name)` for the thread name; each subsystem calls `super("Traffic", bus, clock, 500)`.

### Abstraction and abstract classes

```java
// core/AbstractSubsystem.java
protected abstract void tick();

// model/Vehicle.java
public abstract int getPriority();
```
`Vehicle` cannot be created with `new Vehicle()`; only concrete children can.

### Polymorphism (one call, different behaviour)

* `Vehicle.getPriority()`: `ParkingLot.waiting` (a `PriorityQueue<Vehicle>`) calls `compareTo`, which calls `getPriority()`; each subclass answers differently.
* `CityController.statusText`: `for (Subsystem s : subsystems) sb.append(s.statusReport())`. Three different classes, one loop.
* `CityController.start`: `s.start()` on each `Subsystem`.
* `Intersection.step`: `strategy.chooseNextGreen(...)` runs Fixed or Adaptive depending on the object.
* `EventBus.deliver`: `listener.onEvent(event)` runs the right subsystem's code.
* `ParkingLot.pricing.pricePerHour(...)`.
* `instanceof` and casts: `TrafficSubsystem.tick` (`v instanceof Ambulance`), `ParkingLot.tick` (`v instanceof Chargeable`), `ParkingSubsystem.onEvent` (`(Vehicle) e.getPayload()`).

### Interfaces

| Interface | Implemented by | Purpose |
|---|---|---|
| `core/Subsystem` | `AbstractSubsystem` | Controller treats all subsystems the same |
| `events/CityEventListener` | `AbstractSubsystem` | Can receive events |
| `traffic/SignalStrategy` | `FixedTimerStrategy`, `AdaptiveStrategy` | Swap signal logic |
| `parking/PricingStrategy` | `DynamicPricing` | Swap pricing logic |
| `model/Chargeable` | `ElectricCar` | "Can use a charging slot" |
| `java.lang.Comparable<Vehicle>` | `Vehicle` | Ordering in `PriorityQueue` |
| `java.lang.Runnable` | `SimulationClock`, `EventBus`, `RepairCrew` | Thread body |

`AbstractSubsystem implements Subsystem, CityEventListener` shows a class implementing **two** interfaces. `ElectricCar extends Car implements Chargeable` shows (multilevel) inheritance plus an interface together. `Chargeable` also has a **default method** `isFullyCharged()`, a method with a body inside an interface.

A small trick: `Subsystem` declares `getName()` and `start()`, but does not write them. `Thread` already has them, so `AbstractSubsystem` satisfies the interface automatically.

### Static

* `Vehicle.nextNumber` (static counter shared by all vehicles) and `private static synchronized int takeNextNumber()`:
  ```java
  private static synchronized int takeNextNumber() { return nextNumber++; }
  ```
  `static synchronized` locks the class, so vehicles made on different threads get unique ids.
* `CityController.instance` and `static synchronized getInstance()` (Singleton); `CityController.AREAS` (static final array).
* `VehicleFactory.randomVehicle()` (static method, constructor private).
* `SlotType.allowedFor(...)` (static method inside an enum).
* Constants: `Intersection.CARS_PER_STEP`, `TrafficSubsystem.AMBULANCE_TRAVEL_TICKS`, `StreetLight.MAX_WATTS`, `EventBus.LOG_SIZE`.
* `CityMapPanel.colorFor(...)` (static helper), `UrbanFlowApp.main`.
* Shared `static final Random RANDOM` in `ElectricCar`, `StreetLight`, `ParkingLot`, `VehicleFactory`.

### Nested / inner classes

There is one **static nested class**: `private static class WaitStats` inside `traffic/TrafficSubsystem.java` (fields `carsPassed`, `totalWaitSteps`; methods `record(int)` and `average()`). It is `static`, so it needs no `TrafficSubsystem` object and cannot see its instance fields. `TrafficSubsystem` keeps two of them: `fixedStats` and `adaptiveStats`.

There is no non-static (inner) member class. The inner-class use is an **anonymous inner class** in the `gui/CityDashboard.java` constructor:
```java
addWindowListener(new WindowAdapter() {
    @Override
    public void windowClosing(WindowEvent e) { ... }
});
```
Be honest about this in the viva if asked.

### Custom exceptions (throw / throws / try / catch)

| Exception | Thrown in | Caught in |
|---|---|---|
| `RoadBlockedException` | `Intersection.arrive` (`throws RoadBlockedException`) | `TrafficSubsystem.tick` (count a diverted car); `driveAmbulances` (empty catch, never happens) |
| `ParkingFullException` | `ParkingLot.park` | `ParkingSubsystem.findParking` (multi-catch) |
| `LotClosedException` | `ParkingLot.park` | `ParkingSubsystem.findParking` (multi-catch) |
| `SlotAlreadyTakenException` | `ParkingSlot.occupy` | `ParkingLot.park` (turns it into `IllegalStateException`), `admitWaitingVehicles` (puts the car back in the waiting list) |

```java
// parking/ParkingLot.java  park(Vehicle v) throws ParkingFullException, LotClosedException
if (closed && !v.isEmergency()) { throw new LotClosedException(name); }
ParkingSlot slot = findFreeSlot(v.getType());
if (slot == null) { throw new ParkingFullException(name, v.getId()); }
```

Other exception handling:
* `InterruptedException` (checked, from `sleep`/`wait`): `AbstractSubsystem.run`, `SimulationClock.run`, `EventBus.run`, `RepairCrew.run`. `main` and `runConsoleDemo` declare `throws InterruptedException`.
* Safety nets that catch `RuntimeException`: `AbstractSubsystem.run` (one bad tick does not kill the thread) and `EventBus.deliver` (one bad listener does not stop the bus).

### Collections

| Collection | Where | Why |
|---|---|---|
| `ArrayList` | `TrafficSubsystem.intersections`, `ambulancesOnRoad`; `LightingSubsystem.lights`; `ParkingSubsystem.lots`; `ParkingLot.slots`; `EventBus` listener lists | ordered, indexed lists |
| `LinkedList` (FIFO queue) | `Intersection.queues` (`offer` / `peek` / `poll`), `EventBus.queue` (`offer` / `poll`) and `recentLog`, `RepairCrew.jobs` (`offer` / `poll`) | used as a **queue**: `offer` adds at the back, `peek` looks at the front without removing, `poll` removes the front (null if empty) |
| `HashMap` | `EventBus.listeners`, `Intersection.queues`, `getQueueLengths`, `LightingSubsystem.ambulanceTicks` and `carsPassing` | look up by key |
| `HashSet` | `LightingSubsystem.accidentAreas`, `ParkingSubsystem.spilloverAlerted` | "is X in the set" |
| `PriorityQueue` | `ParkingLot.waiting` | highest priority vehicle first |
| `Iterator` (explicit) | `LightingSubsystem.countDownAmbulanceTicks`: `Iterator<Map.Entry<String, Integer>> it = ambulanceTicks.entrySet().iterator();` with `hasNext()`, `next()`, `it.remove()`, `entry.setValue(...)` | removing while looping is only safe through the iterator |
| Iterator (implicit) | every enhanced `for (X x : list)` loop uses an iterator behind the scenes | |

```java
// events/EventBus.java deliver(): iterate over a COPY so subscribe() cannot disturb the loop
copy = new ArrayList<>(list);
for (CityEventListener listener : copy) { listener.onEvent(event); }
```
Removing during a loop is avoided in two ways: collect first and remove after (`driveAmbulances`: `arrived` list then `removeAll`), or use `it.remove()` on an explicit iterator (`countDownAmbulanceTicks`). A for-each loop that removes would throw `ConcurrentModificationException`.

### Multithreading and synchronization

* Threads: see the table in section 2.
* `synchronized` methods and blocks: `Intersection`, `ParkingLot`, `StreetLight`, `SimulationClock`, `EventBus`, `RepairCrew`, `LightingSubsystem`, and the statistics in `TrafficSubsystem` / `ParkingSubsystem`.
* `Thread.sleep` and `InterruptedException`: loops in every thread.
* `wait()` / `notify()`: `EventBus` and `RepairCrew` (section 7).
* `volatile`: `running` flags.
* Details in section 7.

### Swing GUI

* `CityDashboard extends JFrame`: `BorderLayout` (map in CENTER, status panel EAST, buttons + log SOUTH), `JButton`, `JComboBox<String>`, `JTextArea` inside `JScrollPane`, `JOptionPane.showMessageDialog` for the report.
* `CityMapPanel extends JPanel`: overrides `paintComponent(Graphics)` and draws with `Graphics2D` (roads, lights, queued cars as small squares, ambulance, lots).
* `javax.swing.Timer(200, e -> refresh())` repaints and refreshes the text 5 times a second.
* `SwingUtilities.invokeLater(() -> new CityDashboard(...))` in `main` creates the window on the Swing thread.
* The GUI only **reads** the simulation (getters returning copies) and **writes** only through the five `CityController` operator methods.

### Design patterns

| Pattern | Where | In one line |
|---|---|---|
| **Singleton** | `core/CityController` | private constructor + `static synchronized getInstance()`; only one city |
| **Observer** (publish-subscribe) | `events/EventBus` + `CityEventListener` | subsystems subscribe, bus notifies |
| **Strategy** | `traffic/SignalStrategy` (Fixed, Adaptive); `parking/PricingStrategy` (DynamicPricing) | swap an algorithm at runtime without touching the user class |
| **Factory** | `model/VehicleFactory.randomVehicle()` | caller does not choose the concrete class |
| **Template Method** | `core/AbstractSubsystem.run()` | base class fixes the loop; subclasses fill `tick()` |

```java
// Singleton (core/CityController.java)
private CityController() { ... }
public static synchronized CityController getInstance() {
    if (instance == null) { instance = new CityController(); }
    return instance;
}

// Strategy being swapped at runtime (traffic/TrafficSubsystem.toggleSignalMode)
currentStrategy = (currentStrategy == adaptive) ? fixed : adaptive;
for (Intersection in : intersections) { in.setStrategy(currentStrategy); }
```

Also worth knowing: **fluent chaining** (`addSlots` returns `this`, so `new ParkingLot(...).addSlots(...).addSlots(...)`) and `final` (class `CityController`, `CityEvent`, `VehicleFactory`; method `AbstractSubsystem.run`).

---

## 7. Threads and synchronization, explained simply

### 7.1 Why do we need `synchronized`?

Several threads use the same objects at the same time:

| Object | Threads that touch it |
|---|---|
| `Intersection` | Traffic thread (`arrive`, `step`), EventBus thread (`setBlocked`), Swing thread (drawing, `preempt` from the Dispatch button) |
| `ParkingLot` | Parking thread (`tick`), EventBus thread (`park` via `findParking`), Swing thread (getters for drawing) |
| `StreetLight` | Lighting thread, RepairCrew thread (`repair`), Swing thread (`getMode`) |
| `SimulationClock` | clock thread writes, everyone else reads |

Without a lock, two threads could do this at the same moment:

```
Thread A: finds slot M-ST1 free       Thread B: finds slot M-ST1 free
Thread A: puts car 1 in M-ST1         Thread B: puts car 2 in M-ST1   <- two cars in one slot!
```

`synchronized` on a method means: **take this object's lock before running; if another thread holds it, wait**. Only one thread at a time runs any synchronized method of the same object. Two different intersections have different locks, so they can run in parallel.

`ParkingSlot` has no `synchronized` because it is only used inside `ParkingLot`'s synchronized methods (its class comment says so). As a belt-and-braces safety check `ParkingSlot.occupy` still throws `SlotAlreadyTakenException` if it is ever occupied twice.

### 7.2 How deadlock is avoided

A deadlock needs two threads each holding one lock and waiting for the other's. The code avoids it with three simple rules:

1. **Never call out to another subsystem while holding a lock.**
   * `LightingSubsystem.tick`: the faulty lights are collected inside the `synchronized (this)` block, but `crew.reportFault(...)` and `bus.publish(...)` are called **after** the block ("report outside our lock").
   * `EventBus.run`: takes the event inside `synchronized (this)`, then calls `deliver(event)` **outside** (comment: "Deliver OUTSIDE the lock, so a slow listener never blocks publishers"). `deliver` takes the lock only briefly to copy the listener list.
   * `RepairCrew.run`: sleeps for the repair time **outside** the lock.
   * `SimulationClock.run`: updates time in a short `synchronized` block and publishes events after it.
   * `ParkingSubsystem.findParking` publishes `CAR_REROUTED` after `park` has already returned (lot lock released).
2. **Locks only nest in one direction.** The only nested locks are:
   * `TrafficSubsystem.toggleSignalMode` (traffic lock, then each intersection's lock);
   * `LightingSubsystem.tick` and the energy getters (lighting lock, then each `StreetLight`'s lock);
   * `ParkingLot` methods (lot lock, then vehicle locks such as `ElectricCar.charge`).

   Nothing ever goes the other way (a `StreetLight`, `Intersection` or `Vehicle` never asks for its parent's lock), so no cycle can form.
3. **Statistics use separate short locks.** In `TrafficSubsystem.tick`, `in.waitedSteps(v)` finishes (releasing the intersection lock) before `recordPassed(...)` takes the traffic lock.

Also: `CityEvent` is immutable, so it can be shared between threads with no lock at all.

### 7.3 Producer-consumer with wait()/notify(): RepairCrew, line by line

Idea: the **producer** (Lighting thread) adds broken lights to a list. The **consumer** (RepairCrew thread) takes one at a time. When the list is empty the consumer should **sleep without using CPU**, and be woken when a job arrives.

```java
// PRODUCER - lighting/RepairCrew.java
public synchronized void reportFault(StreetLight light) {
    jobs.offer(light);        // (1) add the job at the back of the queue
    notify();                 // (2) wake the crew thread if it is waiting
}
```

```java
// CONSUMER - the crew's own thread
public void run() {
    while (running) {
        StreetLight light;
        synchronized (this) {                       // (3) take the lock
            while (jobs.isEmpty() && running) {     // (4) nothing to do?
                try {
                    wait();                         // (5) release the lock and sleep
                } catch (InterruptedException e) {
                    return;                         // (6) shutdown() interrupted us
                }
            }
            if (!running) { return; }               // (7) woken because of shutdown
            light = jobs.poll();                    // (8) take the oldest job (front of the queue)
            currentJob = light.getId();
        }                                           // (9) lock released
        try {
            Thread.sleep(REPAIR_TIME_MS);           // (10) "travel and repair" = 4000 ms, NO lock held
        } catch (InterruptedException e) {
            return;                                 // shutdown: the finally block below still runs first
        } finally {
            synchronized (this) { currentJob = null; }   // runs on normal finish AND on that return
        }
        light.repair();                             // (11) set mode OFF; lighting sets the right mode next tick
        synchronized (this) { repairsDone++; }
        bus.publish(EventType.LIGHT_REPAIRED, ...); // (12) outside the lock
    }
}
```

Walk-through:

1. `reportFault` is `synchronized`, so adding to the `LinkedList` is safe.
2. `notify()` wakes **one** thread that is waiting on this object (here only the crew thread can be). `notify`/`wait` may only be called while holding the lock, which is why both sit inside synchronized code.
3. In `run`, the crew enters a synchronized block to look at the list.
4. `while` (not `if`): after waking up, check again. A thread can wake up for another reason (for example shutdown), so it must re-check the condition.
5. `wait()` does two things at once: it **releases the lock** (so `reportFault` can enter) and **puts the thread to sleep**. When notified it must get the lock back before continuing.
6. `InterruptedException`: `shutdown()` calls `worker.interrupt()`, which wakes a waiting thread with this exception, and the thread returns (ends).
7. If woken but `running` is false, also exit.
8. `poll()` removes the front of the queue: first-come first-served.
9. Leaving the block releases the lock.
10. The slow part (sleep) is outside the lock. The `finally` block clears `currentJob` whether the sleep finished or was interrupted (a `return` inside the `catch` does not skip `finally`). Also, so the Lighting thread can still call `reportFault` while a repair is in progress (jobs queue up; `getPendingJobs()` shows how many).
11. `light.repair()` uses `StreetLight`'s own lock.
12. Event published after all locks are released.

`EventBus` is the same pattern with a different list: `publish(CityEvent)` is the producer (`queue.offer`, then `notify()`), and `EventBus.run` is the consumer (`wait()` while `queue.isEmpty()`, then `queue.poll()`). Any thread can be a producer; one thread is the consumer. `shutdown()` uses `notifyAll()` and `interrupt()`.

### 7.4 `volatile` in one paragraph

`private volatile boolean running` is used in `AbstractSubsystem`, `SimulationClock`, `EventBus`, `RepairCrew` (and `echoToConsole` in `EventBus`). One thread writes `running = false` in `shutdown()`; another thread reads it in `while (running)`. Without `volatile` the reading thread might keep using an old cached copy and never stop. `volatile` guarantees the reader sees the latest value. It does **not** make things like `count++` safe; that is why counters use `synchronized`.

### 7.5 Interrupt and clean shutdown

`CityController.shutdown()` calls `shutdown()` on every subsystem, then waits for each with `((Thread) s).join(1000)` (join with a 1 s timeout; `s` is typed `Subsystem`, so it is downcast to `Thread` after an `instanceof Thread` check), then calls `clock.shutdown()` and `bus.shutdown()`. Each sets `running = false` and interrupts the thread so that a sleeping/waiting thread wakes up immediately (`InterruptedException`) and returns.

---

## 8. Beyond-the-syllabus features

| Feature | Where | Plain explanation |
|---|---|---|
| **HashMap / HashSet** | `EventBus.listeners`; `Intersection.queues`; `LightingSubsystem.accidentAreas`, `ambulanceTicks`, `carsPassing`; `ParkingSubsystem.spilloverAlerted` | A `HashMap` stores key-value pairs and finds a value by its key very fast (like a dictionary). A `HashSet` stores each item once and answers "is it in here?" quickly. Values like `int` are boxed into `Integer` automatically (autoboxing). |
| **Enums with fields and methods** | `EventType(boolean logged)`, `LightMode(int brightness)`, `Direction.next()`, `SlotType.allowedFor(...)`, `VehicleType` | An enum is a fixed list of named constants. Java lets each constant carry data (`DIM(30)`) and the enum can have methods. `ordinal()` is the constant's position (0, 1, 2...). `Direction.next()` uses it: `values()[(ordinal() + 1) % 4]`. Enums can be used in `switch`. |
| **wait() / notify()** | `EventBus`, `RepairCrew` | A thread can say "I have nothing to do, put me to sleep until someone tells me there is work". See 7.3. |
| **volatile** | `running` flags | Makes sure all threads see the newest value of a variable. See 7.4. |
| **PriorityQueue + Comparable / compareTo** | `Vehicle.compareTo`, `ParkingLot.waiting` | A `PriorityQueue` always gives you the "smallest" item first. `compareTo` defines what "smaller" means. Here: higher priority number first, then lower id number: `return other.getPriority() - this.getPriority();` then `this.number - other.number`. |
| **Lambdas** | GUI only: `CityDashboard` buttons, `Timer`; `UrbanFlowApp.main` (`SwingUtilities.invokeLater`) | A lambda is a short way to write an object with one method. See below. |
| **Varargs** | `EventBus.subscribe(CityEventListener listener, EventType... types)` | `EventType...` means "any number of EventTypes". Inside the method it is an array. So `bus.subscribe(this, A, B, C)` works without building an array. |
| **Multi-catch** | `ParkingSubsystem.findParking`: `catch (ParkingFullException \| LotClosedException e)` | One catch block for two exception types that need the same handling. The `\|` means "or". |
| **Screenshot helper** | `CityDashboard.scheduleScreenshot` | Testing only. Draws the window into a `BufferedImage`, saves it with `ImageIO.write`, then exits. Run with `--screenshot file.png 20`. Not part of the city logic. |

### Lambda next to the equivalent anonymous class

Lambda (what the code uses, `CityDashboard.buildBottomPanel`):
```java
ambulanceButton.addActionListener(e -> city.dispatchAmbulance());
```
Same thing the long way (an anonymous inner class, which the syllabus covers):
```java
ambulanceButton.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        city.dispatchAmbulance();
    }
});
```
`e ->` means "a method that receives an event called `e`". It works because `ActionListener` has exactly one method.

The same idea for the timer:
```java
Timer refresh = new Timer(200, e -> refresh());     // every 200 ms call refresh()
```
and for starting the GUI safely on the Swing thread:
```java
SwingUtilities.invokeLater(() -> new CityDashboard(city, screenshot, delay));
// () -> ... is "a Runnable with no parameters"
```
The window-close handler in the same class is written the long way, as `new WindowAdapter() { ... }`, because that is a class with many methods, not a lambda candidate.

---

## 9. Likely viva questions with model answers

### Design

**1. What is UrbanFlow in one sentence?**
A multithreaded simulation where traffic signals, street lights and parking lots react to each other through a central `CityController` and an `EventBus`, shown on a Swing dashboard.

**2. Why an EventBus instead of subsystems calling each other directly?**
It keeps them loosely coupled. Traffic only says "a car needs parking"; it does not know Parking exists. I can add or change a subsystem without editing the others. This is the Observer pattern.

**3. Why is `CityController` a Singleton?**
There must be exactly one city: one clock, one bus, one set of threads. The private constructor plus `getInstance()` stops anyone creating a second one. `getInstance` is `synchronized` so two threads cannot create two at once.

**4. Why do the three subsystems extend `AbstractSubsystem`?**
They share the same loop (tick, sleep, handle errors, stop on shutdown). Writing it once avoids copying code. It is the Template Method pattern: the base class fixes the steps, the subclass supplies `tick()`, `onEvent()` and `statusReport()`.

**5. Why is `AbstractSubsystem` abstract and not a normal class?**
It has an abstract method `tick()` with no sensible default, and it should never be created on its own. Also `statusReport()` and `onEvent()` are left to the children.

**6. What does the `Subsystem` interface give you if `AbstractSubsystem` already exists?**
The controller depends on the small contract (`start`, `shutdown`, `statusReport`, `getName`), not on the thread details. That is abstraction. The controller's loops `for (Subsystem s : subsystems)` show polymorphism.

**7. Why is `CityEvent` immutable?**
All fields are `final` with no setters. The same event object is read by the producer, the bus thread, the log and several listeners. Because nothing can change it, it is safe to share without locks.

**8. Where is the Strategy pattern, and why use it?**
`SignalStrategy` (Fixed and Adaptive) and `PricingStrategy` (DynamicPricing). `Intersection` only knows the interface. When the operator clicks the mode button, `setStrategy` swaps the object at runtime. Adding a third signal algorithm needs a new class, no change to `Intersection`.

**9. Explain the adaptive signal logic versus the fixed one.**
Fixed: N, E, S, W in a loop, always 10 steps, even if the road is empty. Adaptive: choose the road with the longest queue; green time is `round(30 * myQueue / totalQueued)` clamped to 3-15 steps; and green ends early if that road becomes empty. So no green is wasted on empty roads and busy roads get longer green.

**10. Why does the Factory pattern help here?**
`TrafficSubsystem` calls `VehicleFactory.randomVehicle()` and does not need to know about `Car`, `Bike`, `ElectricCar`, `Truck`. The odds (50/25/17/8%) live in one place, and a new vehicle type needs a change only in the factory.

### OOP concepts

**11. Give an example of polymorphism.**
`Vehicle.getPriority()` is abstract; `Truck` returns 0, `Car` 1, `ElectricCar` 2 or (via `super.getPriority()`) 1 depending on battery, `Ambulance` 10. The `PriorityQueue<Vehicle>` in `ParkingLot` just calls `compareTo` and the right version runs for each object.

**12. Give an example of encapsulation.**
`StreetLight` keeps `mode` and the energy counters private. The only way to change the mode is `applyMode`, which enforces "a faulty light ignores requests until `repair()`". Nobody outside can break that rule.

**13. Abstract class or interface: how did you choose?**
Abstract class when children share code and state (`Vehicle` has id and number; `AbstractSubsystem` has the loop). Interface when unrelated classes need the same ability (`Chargeable` for EVs, `SignalStrategy`, `CityEventListener`). A class can implement many interfaces but extend only one class.

**14. Where do you use `static`?**
`Vehicle.nextNumber` counter shared by all vehicles, `CityController.instance`/`getInstance()`, `VehicleFactory.randomVehicle()`, `SlotType.allowedFor()`, and constants like `Intersection.CARS_PER_STEP`.

**15. Why is `takeNextNumber()` both `static` and `synchronized`?**
Vehicles are created on several threads (Traffic thread, Swing thread for ambulances). `nextNumber++` is not atomic. `static synchronized` locks the class so two vehicles never get the same id.

**16. Why does `ElectricCar` both extend `Car` and implement `Chargeable`?**
It *is a* car (inheritance; the chain Vehicle -> Car -> ElectricCar is multilevel) and it *can do* charging (interface). Java allows one parent class but many interfaces. `ParkingLot.tick` checks `v instanceof Chargeable` and only charges inside an `EV_CHARGING` slot.

**17. Did you use nested or inner classes?**
One static nested class, `WaitStats` inside `TrafficSubsystem` (cars passed and total wait for one signal mode; `static` so it needs no outer object). And one anonymous inner class: the `WindowAdapter` in `CityDashboard` to handle window closing. Button listeners use lambdas, which are a short form of anonymous classes.

**18. Where do you use `Iterator`?**
Explicitly in `LightingSubsystem.countDownAmbulanceTicks`: I loop over the map entries and must remove the finished ones, so I use `it.hasNext()`, `it.next()`, `it.remove()` and `entry.setValue(...)`. Removing inside a for-each loop would throw `ConcurrentModificationException`. Other loops use for-each, which uses an iterator behind the scenes.

**18a. How is a `LinkedList` used as a queue?**
`offer(x)` adds at the back, `peek()` looks at the front without removing (null if empty), `poll()` removes the front (null if empty). Used in `Intersection` (cars waiting), `EventBus` (events) and `RepairCrew` (jobs).

**18b. What does `this(...)` do in `Car`?**
`public Car()` calls `this(VehicleType.CAR, "C-")`, which runs the other (protected) constructor in the same class. That one calls `super(type, idPrefix)`. `this(...)` or `super(...)` must be the first statement. `ElectricCar` uses the protected one through `super(VehicleType.EV, "E-")`.

**18c. What is a default method? Where is one?**
A method with a body inside an interface: `Chargeable.isFullyCharged()` returns `getBatteryPercent() >= 100`. Any class implementing `Chargeable` gets it for free. `ParkingLot.wantsToLeave` calls it after the downcast `((Chargeable) v)`.

### Exceptions

**19. Why do all your exceptions extend `CityException`, and why is it checked (extends `Exception`)?**
One base class lets me catch "any UrbanFlow problem" with one catch, and still catch specific ones first. It is checked because these are expected, recoverable situations (lot full, road blocked); the compiler forces the caller to decide what to do, so I cannot forget the reroute logic. Unchecked (`RuntimeException`) is for programmer bugs.

**20. Why is `SlotAlreadyTakenException` called a safety check?**
`findFreeSlot` only returns free slots, so double occupancy should never happen. `ParkingSlot.occupy` throws anyway as a guard against a future bug. `ParkingLot.park` converts it to `IllegalStateException` ("this means a programming error"); `admitWaitingVehicles` just puts the car back in the waiting list.

**21. What is the difference between `throw` and `throws`?**
`throw` actually raises an exception object (`throw new ParkingFullException(...)`). `throws` in a method signature warns callers that the method may raise it (`park(Vehicle v) throws ParkingFullException, LotClosedException`).

**22. Why does `driveAmbulances` have a catch block for `RoadBlockedException` that does nothing?**
`Intersection.arrive` declares `throws RoadBlockedException`, so the compiler forces me to handle it. For ambulances it can never happen because `arrive` lets emergency vehicles in; the comment in the code says so.

**23. What does the multi-catch do?**
`catch (ParkingFullException | LotClosedException e)` handles both with the same code (remember the message as the reroute reason). Later `catch (CityException e)` in the loop catches any of them with the base class (polymorphism).

### Threads

**24. Why are there multiple threads?**
Each part of a city runs at its own speed and independently: traffic every 0.5 s, lighting and parking every 1 s, a clock, a repair crew, and the event bus. Threads let them run "at the same time" without blocking the GUI.

**25. `extends Thread` or `implements Runnable`: which did you use and why?**
Both. `AbstractSubsystem extends Thread` (the subsystem is a thread, short and simple). `SimulationClock`, `EventBus`, `RepairCrew` implement `Runnable` and create a `Thread` around themselves, which is the usually preferred style because the class could extend something else.

**25a. What do `join(1000)` and `setPriority` do in `CityController`?**
`shutdown()` calls `((Thread) s).join(1000)`: wait up to 1 second for that subsystem thread to finish (the downcast is needed because `s` is typed `Subsystem`). `start()` gives the Traffic thread `Thread.NORM_PRIORITY + 2`; priority is only a hint to the scheduler, not a guarantee.

**25b. In `RepairCrew`, the `catch` returns. Does `finally` still run?**
Yes. `finally` always runs, even when the `try` or `catch` executes `return`. So `currentJob = null` is cleared both after a normal repair and after a shutdown interrupt.

**26. What is a daemon thread and why use it?**
A daemon thread does not stop the JVM from exiting. Our workers are daemons so they never keep the program alive after the window closes.

**27. Why `synchronized`? Give a concrete example.**
Two cars could be assigned to the same slot if two threads run `park` together. `ParkingLot.park` is `synchronized`, so only one thread at a time can find and take a slot. The same for `Intersection` queues and `StreetLight` modes.

**28. How do you avoid deadlock?**
Do not hold a lock while calling something that needs another subsystem's lock: events are published and delivered outside locks; the crew sleeps outside its lock; the only nested locks (traffic then intersection, lighting then light, lot then vehicle) always go in one direction.

**29. Explain producer-consumer in your project.**
`RepairCrew`: Lighting thread (producer) adds a broken light and calls `notify()`; the crew thread (consumer) `wait()`s while the list is empty. `EventBus` is the same with events: any thread publishes, the bus thread delivers.

**30. Why `while (queue.isEmpty()) wait();` and not `if`?**
After waking, the condition must be checked again because the wake-up may be for another reason (shutdown), and in general another thread might have taken the item first.

**31. What is `volatile` and why only on `running`?**
It makes sure a change made by one thread is seen by others. A flag written by `shutdown()` on one thread and read in the loop of another is the textbook case. Counters need `synchronized` instead.

**32. Why does the GUI use a `javax.swing.Timer` and not its own thread?**
Swing components are not thread-safe; only the Swing event thread may touch them. A Swing `Timer` runs its action on that thread. The simulation threads never touch components; the GUI just reads data every 200 ms.

**33. Why does the GUI only read data?**
So drawing can never corrupt the simulation, and the simulation can change without breaking the drawing. The panel uses synchronized getters that return **copies** (`getQueueSnapshot`, `getSlotTypes`...). The only writes are the five button actions, and those go through `CityController`.

### Scenarios and logic

**34. Why are ambulances never blocked?**
Three checks: `Intersection.arrive` lets emergency vehicles in even when blocked; `step()` tests `emergencyRequests > 0` before `blocked`, so green is forced; and `ParkingLot.park` ignores the closed flag for emergency vehicles. `Vehicle.isEmergency()` is false by default and `Ambulance` overrides it to true.

**35. How does the green wave work?**
At dispatch, `preempt(WEST)` is called on INT-1 and INT-2 (and on INT-3 when the ambulance leaves INT-1). `preempt` increments `emergencyRequests`; while it is above 0, `step()` forces green for the ambulance's direction. When the ambulance crosses, the counter goes down and normal signals resume. Note the ambulance still joins the end of the queue, so cars ahead of it clear first, 2 per step.

**36. What happens to a car when the lot it wants is full or closed?**
`findParking`: try the preferred lot, catch `ParkingFullException`/`LotClosedException`, try the other lots (reroute event if one accepts), if all refuse: wait in the preferred lot's priority queue (or the first open lot if preferred is closed); if that queue already has 8 cars, the driver gives up.

**37. What does an accident do?**
One `ACCIDENT_REPORTED` event. Traffic blocks the area's junction (cars diverted by `RoadBlockedException`), Lighting makes the area's lights FLASH, Parking closes the area's lot (`LotClosedException` for new arrivals). `ROAD_REOPENED` undoes all three.

**38. Which wins in the lighting rules if there is both an accident and an ambulance?**
Accident (FLASHING), because it is the first condition in `LightingSubsystem.tick`. Then ambulance (FULL), then day (OFF), then night (DIM, FULL near cars).

**39. How do you show adaptive signals are better?**
Statistics are kept separately for each mode (`passedAdaptive`, `waitAdaptive`, `passedFixed`, `waitFixed`). The End-of-Day Report prints cars passed and average wait for each. With heavy traffic Fixed gives green to empty roads so queues grow. (Be careful: the average wait only counts cars that already got through.)

**40. How is energy saving calculated?**
At night each light adds `150 W * brightness% * hours` to `energyWh`, and `150 W * hours` to `baselineWh` (an old light always at full power). Saving = `100 * (1 - used / baseline)`. Daytime is ignored on both sides so the comparison is fair.

**41. How does dynamic pricing work?**
`DynamicPricing.pricePerHour`: base rate by slot type, times 1.5 if the lot is over 70% full, times 2 if over 90%, plus 0.25 on the multiplier during rush hour. Revenue is added when a car leaves: price times hours parked.

**42. How does the PriorityQueue decide who is admitted first?**
`Vehicle.compareTo`: higher `getPriority()` first (ambulance 10, EV with low battery 2, cars/bikes 1, trucks 0); same priority: lower creation number first (first come, first served).

### Improvement

**43. What would you improve next?**
A real road network (graph) with shortest-path routing for ambulances and diverted cars; saving statistics to a file or database; unit tests; reading city layout and numbers from a configuration file; more than one main road; real sensor data instead of random arrivals.

**43a. Where is a downcast used and why is it safe?**
`(Ambulance) e.getPayload()` in `ParkingSubsystem.onEvent` (`AMBULANCE_ARRIVED` always carries an Ambulance), `((Chargeable) v).isFullyCharged()` after an `instanceof Chargeable` check, and `((Thread) s).join(1000)` after `instanceof Thread`. A wrong downcast would throw `ClassCastException`.

**44. What is the weakest part of your design?**
The road is a single straight line with a fixed ambulance route, arrivals are random, and the results are in "steps" not real seconds. I would replace the fixed route and area-based accidents with a proper map. (More in section 10.)

---

## 10. Known simplifications and future work

Be upfront about these. Saying them first is better than being caught.

### Simplifications

* **One main road, no routing graph.** The three junctions are in a straight line. The ambulance always drives West to East to the hospital. Diverted cars are just counted (`divertedByAccident++`); they are not routed anywhere.
* **Accidents are per area, not per position.** An accident blocks the junction of that area and closes that area's lot. Central has no lot, so a Central accident closes nothing.
* **Random arrivals.** Cars arrive by chance (`random.nextDouble()`), directions and vehicle types are random, so every run is different. No fixed seed.
* **Time units are "signal-seconds".** One intersection step (one traffic tick) is one simulated signal second, but it takes 0.5 *real* seconds. `averageWait` documents this, and the report prints "s". The city clock is a separate scale (10 simulated minutes per real second).
* **Average wait only counts cars that have crossed.** Cars still stuck in a queue at the end are not included, so a badly congested mode can look better than it is. Look at "cars passed" too.
* **Waiting queues at junctions are unlimited** (they can grow forever). The parking waiting queue is limited (8, then drivers give up).
* **Motion sensor is a probability** (`cars * 0.12`), not real car positions.
* **Parking stay time is random:** after 6 ticks, 4% chance per tick to leave.
* **In-memory only.** Nothing is saved to a file or database. When the program ends, the data is gone (only the printed report remains).
* **No automated tests.** Testing was done by running the console demo and the GUI.
* **GUI is tied to this city.** `CityMapPanel` has hard-coded pixel positions for 3 junctions, 9 lights and 2 lots (`INTERSECTION_X`, `LIGHT_X`, `lots.get(0)/get(1)`).
* **Parking events are less frequent now** (only 8% of passing cars look for parking), so plain "lot full" reroutes happen only now and then, mostly at the small Hospital Lot. Closing a lot in an accident gives reroutes reliably.
* **Ambulance reaches the junction at the back of the queue.** Green wave clears the road ahead, but cars already queued still go first.

### Small code oddities to know about (so you are not surprised)

* **Rush hour check comes first.** 19:00-19:59 counts as rush hour for car arrivals (rate 1.5) even though it is also night.
* **Constructors register `this` on the bus** (`bus.subscribe(this, ...)` in each subsystem constructor). It is safe here because events are only delivered after `start()`.
* **`Subsystem` re-declares `getName()` and `start()`** which `Thread` already provides. That is intentional but looks odd.
* **`CityController.dispatchAmbulance` runs on the caller's thread.** From the GUI that is the Swing thread. It is quick (short locks), so it does not freeze the window.

### Ideas for future work

1. Replace the straight road with a graph of junctions and roads; use shortest-path (e.g. Dijkstra) for ambulances and for diverted cars.
2. Add more lots and more areas; add real pricing zones; online parking reservation.
3. Save statistics and event history to a file or database; add charts to the dashboard.
4. Load layout and numbers (tick times, slot counts, probabilities) from a config file.
5. Add unit tests (JUnit) for `Intersection`, `ParkingLot`, `AdaptiveStrategy`, `DynamicPricing`.
6. Support several ambulances with their own routes and a smarter wave (clear the ambulance's own queue position).
7. Replace random arrivals by recorded traffic data, and add pedestrians and public transport priority.
8. Add a new strategy class (for example "time-of-day" signals) to show how easily Strategy extends.
