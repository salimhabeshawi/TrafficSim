package com.trafficflow.model;

import java.util.UUID;

public class Car {
    private final String id;
    private final Direction direction;
    private final String color;

    public Car(Direction direction, String color) {
        this.id = UUID.randomUUID().toString();
        this.direction = direction;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getColor() {
        return color;
    }
}
