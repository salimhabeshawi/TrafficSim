package com.trafficflow.simulation;

import com.trafficflow.model.Car;
import com.trafficflow.model.Direction;
import com.trafficflow.model.PassEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Component
public class Intersection {
    private final Map<Direction, BlockingQueue<Car>> lanes = new EnumMap<>(Direction.class);
    private final ReentrantLock lock = new ReentrantLock();

    public Intersection() {
        for (Direction dir : Direction.values()) {
            lanes.put(dir, new LinkedBlockingQueue<>());
        }
    }

    public void addCar(Car car) {
        lanes.get(car.getDirection()).offer(car);
    }

    public boolean tryPass(Car car, Direction green, long crossingMs, Consumer<PassEvent> onPassStarted) {
        if (car.getDirection() != green) {
            return false;
        }

        BlockingQueue<Car> lane = lanes.get(car.getDirection());
        if (lane.peek() == null || !lane.peek().getId().equals(car.getId())) {
            return false;
        }

        if (!lock.tryLock()) {
            return false;
        }

        try {
            Car front = lane.peek();
            if (front == null || !front.getId().equals(car.getId())) {
                return false;
            }

            lane.poll();
            onPassStarted.accept(new PassEvent(car.getId(), car.getDirection(), car.getColor()));
            Thread.sleep(crossingMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        lanes.values().forEach(BlockingQueue::clear);
    }

    public int totalWaitingCars() {
        int total = 0;
        for (BlockingQueue<Car> lane : lanes.values()) {
            total += lane.size();
        }
        return total;
    }

    public Map<Direction, Integer> queueLengths() {
        Map<Direction, Integer> lengths = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            lengths.put(dir, lanes.get(dir).size());
        }
        return lengths;
    }

    public Map<Direction, List<Car>> waitingCars() {
        Map<Direction, List<Car>> snapshot = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            snapshot.put(dir, new ArrayList<>(lanes.get(dir)));
        }
        return snapshot;
    }
}
