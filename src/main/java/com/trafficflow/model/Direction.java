package com.trafficflow.model;

public enum Direction {
    NORTH,
    SOUTH,
    EAST,
    WEST;

    public Direction next() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }
}
