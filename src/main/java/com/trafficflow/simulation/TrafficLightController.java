package com.trafficflow.simulation;

import com.trafficflow.model.Direction;
import org.springframework.stereotype.Component;

@Component
public class TrafficLightController {
    private volatile Direction greenDirection = Direction.NORTH;

    public Direction getGreenDirection() {
        return greenDirection;
    }

    public synchronized Direction rotate() {
        greenDirection = greenDirection.next();
        return greenDirection;
    }

    public void reset() {
        greenDirection = Direction.NORTH;
    }
}
