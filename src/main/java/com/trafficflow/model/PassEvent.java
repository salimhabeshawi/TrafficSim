package com.trafficflow.model;

public class PassEvent {
    private final String carId;
    private final Direction direction;
    private final String color;
    private final long passedAt;

    public PassEvent(String carId, Direction direction, String color) {
        this.carId = carId;
        this.direction = direction;
        this.color = color;
        this.passedAt = System.currentTimeMillis();
    }

    public String getCarId() {
        return carId;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getColor() {
        return color;
    }

    public long getPassedAt() {
        return passedAt;
    }
}
