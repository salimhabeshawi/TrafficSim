Create a minimal full-stack web application called "TrafficFlow" that simulates a multithreaded traffic intersection in real time.

Tech stack:
- Backend: Java Spring Boot
- Frontend: HTML/CSS/Vanilla JavaScript (no React)
- Communication: WebSocket or Server-Sent Events for live updates
- No database
- Keep architecture simple and lightweight
- Clean folder structure
- Easy to run locally

Main goal:
Demonstrate Java multithreading concepts visually in a web browser.

Core simulation:
- Simulate a 4-way traffic intersection:
  - North
  - South
  - East
  - West

- Cars are generated automatically at random intervals.
- Each car should run as its own task/thread using:
  - ExecutorService
  - Thread pools
  - Concurrent collections

- The intersection is a shared synchronized resource.
- Prevent collisions using proper synchronization primitives:
  - synchronized blocks OR ReentrantLock

- Only one traffic direction should have green at a time.
- Traffic lights automatically rotate every few seconds.

Frontend requirements:
- Very clean minimal UI.
- Draw simple roads and moving colored rectangles as cars.
- Show:
  - current traffic light state
  - queue length per direction
  - total cars passed
  - active threads count

- Use simple animations only.
- No heavy graphics libraries.
- Responsive enough for desktop.

Backend requirements:
- Separate simulation logic from API layer.
- Use:
  - Car class
  - Intersection class
  - TrafficLightController
  - SimulationService

- Use ScheduledExecutorService for:
  - car generation
  - traffic light switching

- Use thread-safe queues:
  - BlockingQueue or ConcurrentLinkedQueue

WebSocket requirements:
- Push live simulation updates from backend to frontend.
- Frontend updates automatically without refresh.

Features:
- Start simulation button
- Pause simulation button
- Reset simulation button
- Adjustable traffic speed slider

Important constraints:
- Keep the project beginner/intermediate friendly.
- Avoid microservices.
- Avoid Docker.
- Avoid Kubernetes.
- Avoid authentication.
- Avoid databases.
- Avoid overengineering.
- Keep total codebase reasonably small.

Code quality:
- Clear comments explaining multithreading logic.
- Explain synchronization decisions.
- Use proper OOP.
- Keep methods short and readable.

Deliver:
1. Complete project structure
2. Backend code
3. Frontend code
4. Step-by-step setup instructions
5. Explanation of how multithreading works in this project
6. Explanation of race conditions prevented
7. Commands to run locally

Bonus (optional if simple):
- Emergency vehicle priority
- Basic collision counter
- Dark mode toggle

The final result should feel like a polished educational concurrency demo, not an enterprise application.