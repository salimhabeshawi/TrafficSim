package com.trafficflow.api;

import com.trafficflow.model.SimulationState;
import com.trafficflow.simulation.SimulationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {
    private final SimulationService service;

    public SimulationController(SimulationService service) {
        this.service = service;
    }

    @PostMapping("/start")
    public SimulationState start() {
        service.start();
        return service.snapshotState();
    }

    @PostMapping("/pause")
    public SimulationState pause() {
        service.pause();
        return service.snapshotState();
    }

    @PostMapping("/reset")
    public SimulationState reset() {
        service.reset();
        return service.snapshotState();
    }

    @PostMapping("/speed")
    public SimulationState setSpeed(@RequestBody Map<String, Double> body) {
        double speed = body.getOrDefault("speed", 1.0);
        service.setSpeed(speed);
        return service.snapshotState();
    }

    @GetMapping("/state")
    public SimulationState getState() {
        return service.snapshotState();
    }
}
