# UrbanFlow — Smart City Traffic, Street Lighting & Parking (Java OOP)

A multithreaded Java simulation where traffic signals, street lights and parking lots
work together through a central city controller, shown live on a Swing dashboard.
Plain Java (JDK 17+), no external libraries, no build tool.

## Run

From this folder (`urbanflow/`):

```bash
javac -d out $(find src -name "*.java")
java -cp out com.urbanflow.UrbanFlowApp                # Swing dashboard
java -cp out com.urbanflow.UrbanFlowApp --console 60   # scripted demo in the terminal
```

PowerShell compile command: `javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName`

## Problem statement (short)

Traffic signals, street lights and parking usually run separately on fixed rules, so one
problem makes another worse: full lots leave cars queuing on the road, ambulances wait at
red lights on dark streets, and an accident needs everything to react at once.
UrbanFlow connects the three through events and handles three cross-system scenarios:

| Scenario | What happens |
|---|---|
| **Lot full** | `ParkingFullException` → the car is rerouted to the other lot; if all are full it waits in a priority queue (or gives up when the queue is too long) |
| **Ambulance** | Green wave: signals ahead hold green; street lights in its area go to full; it parks in the Hospital Lot's reserved EMERGENCY bay |
| **Accident** | Intersection blocked (`RoadBlockedException` diverts cars), lights in the area flash, the area's lot closes (`LotClosedException`) |

## City layout

```
  WEST                    CENTRAL                  EAST
  INT-1 ----------------- INT-2 ----------------- INT-3 --> hospital
  Mall Lot (10 slots)                             Hospital Lot (8 slots incl. emergency bay)
  3 street lights         3 street lights         3 street lights
```

## Architecture

```
                 CityController (Singleton) ── builds the city, operator actions, reports
                        │
        SimulationClock (thread)     EventBus (thread, producer-consumer queue)
                                        │ publish / subscribe
           ┌────────────────────────────┼────────────────────────────┐
   TrafficSubsystem (thread)   LightingSubsystem (thread)    ParkingSubsystem (thread)
   Intersection x3             StreetLight x9                ParkingLot x2, ParkingSlot
   Fixed / Adaptive strategy   RepairCrew (thread)           DynamicPricing, PriorityQueue
```

Subsystems never call each other: they publish events (e.g. `CAR_NEEDS_PARKING`,
`AMBULANCE_MOVED`, `ACCIDENT_REPORTED`) and react in their own `onEvent()`.

## Packages

| Package | Contents |
|---|---|
| `core` | `CityController`, `SimulationClock`, `Subsystem` (interface), `AbstractSubsystem` |
| `events` | `EventBus`, `CityEvent`, `EventType`, `CityEventListener` |
| `model` | `Vehicle` (abstract), `Car`, `Bike`, `Truck`, `ElectricCar`, `Ambulance`, `Chargeable`, `VehicleType`, `VehicleFactory` |
| `traffic` | `TrafficSubsystem`, `Intersection`, `SignalStrategy`, `FixedTimerStrategy`, `AdaptiveStrategy`, `Direction` |
| `lighting` | `LightingSubsystem`, `StreetLight`, `RepairCrew`, `LightMode` |
| `parking` | `ParkingSubsystem`, `ParkingLot`, `ParkingSlot`, `SlotType`, `PricingStrategy`, `DynamicPricing` |
| `exceptions` | `CityException` → `ParkingFullException`, `LotClosedException`, `SlotAlreadyTakenException`, `RoadBlockedException` |
| `gui` | `CityDashboard` (window, buttons, log), `CityMapPanel` (live map drawing) |

41 files: 31 classes, 5 interfaces, 5 enums, about 2,000 lines of code.

## OOP concepts used

| Concept | Where |
|---|---|
| Encapsulation | Private state changed only through checked methods: `StreetLight`, `Intersection`, `ParkingLot` |
| Inheritance | Hierarchical: `Car`/`Bike`/`Truck`/`Ambulance extends Vehicle`; multilevel: `Vehicle → Car → ElectricCar`; subsystems `extends AbstractSubsystem extends Thread`; `super(...)`, `this(...)` chaining, `final run()` |
| Abstraction | abstract `Vehicle.getPriority()`, abstract `AbstractSubsystem.tick()`; interfaces `Subsystem`, `SignalStrategy`, `PricingStrategy`, `Chargeable` |
| Polymorphism | Controller starts/reports any `Subsystem`; signals switch Fixed ↔ Adaptive at runtime; priority differs per vehicle |
| Custom exceptions | Checked `CityException` hierarchy thrown and caught in parking and traffic |
| Interfaces | Default method `Chargeable.isFullyCharged()`; `ElectricCar extends Car implements Chargeable` |
| Static & nested | Static ID counter in `Vehicle`, static nested class `TrafficSubsystem.WaitStats`, anonymous `WindowAdapter`, Singleton `CityController` |
| Collections | `ArrayList`, `LinkedList` as a queue (`offer`/`peek`/`poll`), `Iterator` with `remove()`, `HashMap`/`HashSet`, `PriorityQueue` |
| Exceptions | Checked custom hierarchy, multi-catch, `finally` that runs even on `return` (`RepairCrew`) |
| Multithreading | 6+ threads (`extends Thread` and `implements Runnable`); `synchronized`; `wait()`/`notify()`; `join(1000)` on shutdown; thread priority; `Thread.currentThread()` |
| Swing | `JFrame`, custom `paintComponent`, buttons, `javax.swing.Timer` refresh |
| Design patterns | Singleton, Observer (EventBus), Strategy (signals, pricing), Factory (`VehicleFactory`), Template Method (`AbstractSubsystem.run`) |

See `WALKTHROUGH.md` for a class-by-class explanation, scenario traces and viva questions.
