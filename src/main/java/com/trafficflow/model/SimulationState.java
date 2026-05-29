package com.trafficflow.model;

import java.util.List;
import java.util.Map;

public class SimulationState {
    private final boolean running;
    private final Direction greenDirection;
    private final long totalCarsPassed;
    private final int activeThreads;
    private final double speedMultiplier;
    private final Map<Direction, Integer> queueLengths;
    private final Map<Direction, List<CarView>> waitingCars;
    private final List<PassEvent> passEvents;

    public SimulationState(
            boolean running,
            Direction greenDirection,
            long totalCarsPassed,
            int activeThreads,
            double speedMultiplier,
            Map<Direction, Integer> queueLengths,
            Map<Direction, List<CarView>> waitingCars,
            List<PassEvent> passEvents
    ) {
        this.running = running;
        this.greenDirection = greenDirection;
        this.totalCarsPassed = totalCarsPassed;
        this.activeThreads = activeThreads;
        this.speedMultiplier = speedMultiplier;
        this.queueLengths = queueLengths;
        this.waitingCars = waitingCars;
        this.passEvents = passEvents;
    }

    public boolean isRunning() {
        return running;
    }

    public Direction getGreenDirection() {
        return greenDirection;
    }

    public long getTotalCarsPassed() {
        return totalCarsPassed;
    }

    public int getActiveThreads() {
        return activeThreads;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public Map<Direction, Integer> getQueueLengths() {
        return queueLengths;
    }

    public Map<Direction, List<CarView>> getWaitingCars() {
        return waitingCars;
    }

    public List<PassEvent> getPassEvents() {
        return passEvents;
    }

    public record CarView(String id, String color) {
    }
}
