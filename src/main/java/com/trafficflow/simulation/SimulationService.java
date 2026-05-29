package com.trafficflow.simulation;

import com.trafficflow.model.Car;
import com.trafficflow.model.Direction;
import com.trafficflow.model.PassEvent;
import com.trafficflow.model.SimulationState;
import com.trafficflow.ws.SimulationWebSocketHandler;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SimulationService {
    private static final int MAX_WAITING_CARS = 40;
    private static final long BASE_LIGHT_INTERVAL_MS = 4_000;

    private static final List<String> CAR_COLORS = List.of(
            "#ef4444", "#22c55e", "#3b82f6", "#f59e0b", "#06b6d4", "#ec4899"
    );

    private final Intersection intersection;
    private final TrafficLightController trafficLightController;
    private final SimulationWebSocketHandler socketHandler;
    private final ThreadPoolExecutor carExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final AtomicLong totalCarsPassed = new AtomicLong();
    private final AtomicInteger resetVersion = new AtomicInteger();
    private final Queue<PassEvent> passEvents = new ConcurrentLinkedQueue<>();

    private volatile boolean running = false;
    private volatile double speedMultiplier = 1.0;
    private volatile long lastLightRotationAt = System.currentTimeMillis();
    private boolean initialized = false;

    public SimulationService(
            SimulationWebSocketHandler socketHandler,
            Intersection intersection,
            TrafficLightController trafficLightController) {
        this.socketHandler = socketHandler;
        this.intersection = intersection;
        this.trafficLightController = trafficLightController;
        initSchedulers();
    }

    private synchronized void initSchedulers() {
        if (!initialized) {
            initialized = true;
            scheduler.scheduleAtFixedRate(this::generateCars, 0, 350, TimeUnit.MILLISECONDS);
            scheduler.scheduleAtFixedRate(this::rotateLights, 200, 200, TimeUnit.MILLISECONDS);
            scheduler.scheduleAtFixedRate(this::broadcastState, 0, 150, TimeUnit.MILLISECONDS);
        }
    }

    public void start() {
        running = true;
        broadcastState();
    }

    public void pause() {
        running = false;
        broadcastState();
    }

    public void reset() {
        running = false;
        resetVersion.incrementAndGet();
        totalCarsPassed.set(0);
        passEvents.clear();
        trafficLightController.reset();
        intersection.clear();
        lastLightRotationAt = System.currentTimeMillis();
        broadcastState();
    }

    public void setSpeed(double value) {
        speedMultiplier = Math.max(0.5, Math.min(3.0, value));
        broadcastState();
    }

    public SimulationState snapshotState() {
        Map<Direction, List<SimulationState.CarView>> waitingCarViews = new EnumMap<>(Direction.class);
        Map<Direction, List<Car>> waitingCars = intersection.waitingCars();
        
        for (Direction direction : Direction.values()) {
            List<SimulationState.CarView> views = waitingCars.get(direction).stream()
                    .map(car -> new SimulationState.CarView(car.getId(), car.getColor()))
                    .toList();
            waitingCarViews.put(direction, views);
        }

        List<PassEvent> events = drainPassEvents();

        return new SimulationState(
                running,
                trafficLightController.getGreenDirection(),
                totalCarsPassed.get(),
                carExecutor.getActiveCount(),
                speedMultiplier,
                intersection.queueLengths(),
                waitingCarViews,
                events
        );
    }

    private void generateCars() {
        if (!running) {
            return;
        }

        if (intersection.totalWaitingCars() >= MAX_WAITING_CARS) {
            return;
        }

        if (ThreadLocalRandom.current().nextDouble() > (0.20 * speedMultiplier)) {
            return;
        }

        Direction direction = Direction.values()[ThreadLocalRandom.current().nextInt(Direction.values().length)];
        String color = CAR_COLORS.get(ThreadLocalRandom.current().nextInt(CAR_COLORS.size()));
        
        int carResetVersion = resetVersion.get();

        Car car = new Car(direction, color);
        intersection.addCar(car);
        carExecutor.submit(() -> runCar(car, carResetVersion));
    }

    private void runCar(Car car, int carResetVersion) {
        while (carResetVersion == resetVersion.get() && !Thread.currentThread().isInterrupted()) {
            if (!running) {
                if (!sleep(100)) {
                    break;
                }
                continue;
            }

            long crossingMillis = Math.max(180, (long) (500 / speedMultiplier));
            boolean passed = intersection.tryPass(
                    car,
                    trafficLightController.getGreenDirection(),
                    crossingMillis,
                    this::recordPassStart
            );
            
            if (passed) {
                break;
            }

            long retryDelay = Math.max(50, (long) (120 / speedMultiplier));
            if (!sleep(retryDelay)) {
                break;
            }
        }
    }

    private void rotateLights() {
        if (!running) {
            lastLightRotationAt = System.currentTimeMillis();
            return;
        }

        long now = System.currentTimeMillis();
        long interval = Math.max(1_300, (long) (BASE_LIGHT_INTERVAL_MS / speedMultiplier));

        if (now - lastLightRotationAt >= interval) {
            trafficLightController.rotate();
            lastLightRotationAt = now;
        }
    }

    private void broadcastState() {
        socketHandler.broadcast(snapshotState());
    }

    private void recordPassStart(PassEvent event) {
        totalCarsPassed.incrementAndGet();
        passEvents.offer(event);
    }

    private List<PassEvent> drainPassEvents() {
        List<PassEvent> events = new ArrayList<>();
        PassEvent event;

        while ((event = passEvents.poll()) != null) {
            events.add(event);
        }

        return events;
    }

    private boolean sleep(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
