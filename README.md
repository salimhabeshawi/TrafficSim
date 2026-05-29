# TrafficFlow

TrafficFlow is a minimal full-stack educational demo that visualizes Java multithreading concepts in a 4-way traffic intersection.

## Tech Stack

- Backend: Java 17 + Spring Boot
- Frontend: HTML, CSS, Vanilla JavaScript
- Live updates: Native WebSocket

## Project Structure

```
TrafficSim/
  pom.xml
  README.md
  src/main/java/com/trafficflow/
    TrafficFlowApplication.java
    api/SimulationController.java
    config/WebSocketConfig.java
    model/
      Car.java
      Direction.java
      PassEvent.java
      SimulationState.java
    simulation/
      Intersection.java
      TrafficLightController.java
      SimulationService.java
    ws/SimulationWebSocketHandler.java
  src/main/resources/
    application.properties
    static/
      index.html
      style.css
      app.js
```

## How Multithreading Works Here

1. `SimulationService` owns:
   - a fixed thread pool (`ExecutorService`) for car tasks
   - a scheduled thread pool (`ScheduledExecutorService`) for timed jobs
2. Cars are generated at random intervals while running.
3. Each generated car is submitted as its own task (`runCar`) to the car executor.
4. Car tasks repeatedly try to pass through the shared `Intersection`.
5. Traffic lights rotate every few seconds through NORTH -> EAST -> SOUTH -> WEST.
6. A broadcast scheduler pushes simulation snapshots over WebSocket so the UI updates in real time.

## Synchronization and Race Conditions Prevented

- Shared resource: the intersection crossing area.
- `Intersection` uses `ReentrantLock` to ensure only one car occupies the crossing at a time.
- Lane queues are `LinkedBlockingQueue` instances (thread-safe) to avoid queue corruption under concurrent access.
- Before a car crosses, it verifies it is still at the head of its lane queue; this prevents out-of-order passing when multiple car threads race.
- Simulation counters and flags use `Atomic*` types for safe concurrent reads/writes.

Without these guards, common race issues would include:
- two cars entering at once (collision)
- incorrect queue removals
- stale traffic-light reads
- lost increments in passed-car counters

## Setup and Run

## 1) Prerequisites

- Java 17+
- Maven 3.9+

## 2) Run the app

From project root:

```bash
mvn spring-boot:run
```

Then open:

```
http://localhost:8080
```

## 3) Build jar (optional)

```bash
mvn clean package
java -jar target/trafficflow-0.0.1-SNAPSHOT.jar
```

## Controls

- Start simulation
- Pause simulation
- Reset simulation
- Adjust speed with slider (`0.5x` to `3.0x`)