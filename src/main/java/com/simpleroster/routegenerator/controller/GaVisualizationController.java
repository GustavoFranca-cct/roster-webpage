package com.simpleroster.routegenerator.controller;

import com.simpleroster.routegenerator.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/ga")
public class GaVisualizationController {

    private final ScheduleService scheduleService;
    private final Map<String, GaVisualizationState> visualizationStates = new ConcurrentHashMap<>();

    @Autowired
    public GaVisualizationController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startVisualization(
            @RequestParam(required = false) Integer populationSize,
            @RequestParam(required = false) Integer maxGenerations,
            @RequestParam(required = false) Double mutationRate,
            @RequestParam(required = false) Double crossoverRate,
            @RequestParam(required = false) Integer tournamentSize,
            @RequestParam(required = false) Integer penaltyHardConstraint,
            @RequestParam(required = false) Integer penaltyUnderStaffing,
            @RequestParam(required = false) Integer penaltyOverStaffing,
            @RequestParam(required = false) Integer penaltyForbiddenSuccession) {
        
        // Generate a unique ID for this visualization
        String visualizationId = java.util.UUID.randomUUID().toString();
        
        // Create a new state for this visualization
        GaVisualizationState state = new GaVisualizationState();
        state.setPopulationSize(populationSize != null ? populationSize : 50);
        state.setMaxGenerations(maxGenerations != null ? maxGenerations : 100);
        state.setMutationRate(mutationRate != null ? mutationRate : 0.1);
        state.setCrossoverRate(crossoverRate != null ? crossoverRate : 0.8);
        state.setTournamentSize(tournamentSize != null ? tournamentSize : 5);
        state.setPenaltyHardConstraint(penaltyHardConstraint != null ? penaltyHardConstraint : 500);
        state.setPenaltyUnderStaffing(penaltyUnderStaffing != null ? penaltyUnderStaffing : 350);
        state.setPenaltyOverStaffing(penaltyOverStaffing != null ? penaltyOverStaffing : 30);
        state.setPenaltyForbiddenSuccession(penaltyForbiddenSuccession != null ? penaltyForbiddenSuccession : 200);
        
        // Store the state
        visualizationStates.put(visualizationId, state);
        
        // Start the visualization in a separate thread
        new Thread(() -> runVisualization(visualizationId)).start();
        
        // Return the visualization ID
        Map<String, Object> response = new HashMap<>();
        response.put("visualizationId", visualizationId);
        response.put("message", "GA visualization started");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        // For now, just return a mock status
        // In a real implementation, you would track the actual GA progress
        Map<String, Object> status = new HashMap<>();
        
        // Check if there are any active visualizations
        if (visualizationStates.isEmpty()) {
            status.put("active", false);
            status.put("message", "No active visualization");
            return ResponseEntity.ok(status);
        }
        
        // Get the first active visualization (in a real app, you'd handle multiple)
        GaVisualizationState state = visualizationStates.values().iterator().next();
        
        // Update the state
        state.setCurrentGeneration(state.getCurrentGeneration() + 1);
        
        // Check if we're done
        boolean completed = state.getCurrentGeneration() >= state.getMaxGenerations();
        if (completed) {
            visualizationStates.remove(state.getVisualizationId());
        }
        
        // Create mock fitness data
        double bestFitness = 1000.0 - (state.getCurrentGeneration() * 10.0);
        double avgFitness = 1200.0 - (state.getCurrentGeneration() * 8.0);
        double worstFitness = 1500.0 - (state.getCurrentGeneration() * 5.0);
        
        // Return the status
        status.put("active", true);
        status.put("visualizationId", state.getVisualizationId());
        status.put("currentGeneration", state.getCurrentGeneration());
        status.put("totalGenerations", state.getMaxGenerations());
        status.put("completed", completed);
        status.put("fitnessData", Map.of(
            "best", bestFitness,
            "average", avgFitness,
            "worst", worstFitness
        ));
        
        return ResponseEntity.ok(status);
    }
    
    private void runVisualization(String visualizationId) {
        // In a real implementation, this would run the actual GA
        // For now, we just simulate progress through the status endpoint
        try {
            Thread.sleep(1000); // Simulate some work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Inner class to track visualization state
    private static class GaVisualizationState {
        private String visualizationId = java.util.UUID.randomUUID().toString();
        private int populationSize = 50;
        private int maxGenerations = 100;
        private double mutationRate = 0.1;
        private double crossoverRate = 0.8;
        private int tournamentSize = 5;
        private int penaltyHardConstraint = 500;
        private int penaltyUnderStaffing = 350;
        private int penaltyOverStaffing = 30;
        private int penaltyForbiddenSuccession = 200;
        private int currentGeneration = 0;
        
        // Getters and setters
        public String getVisualizationId() { return visualizationId; }
        public void setVisualizationId(String visualizationId) { this.visualizationId = visualizationId; }
        public int getPopulationSize() { return populationSize; }
        public void setPopulationSize(int populationSize) { this.populationSize = populationSize; }
        public int getMaxGenerations() { return maxGenerations; }
        public void setMaxGenerations(int maxGenerations) { this.maxGenerations = maxGenerations; }
        public double getMutationRate() { return mutationRate; }
        public void setMutationRate(double mutationRate) { this.mutationRate = mutationRate; }
        public double getCrossoverRate() { return crossoverRate; }
        public void setCrossoverRate(double crossoverRate) { this.crossoverRate = crossoverRate; }
        public int getTournamentSize() { return tournamentSize; }
        public void setTournamentSize(int tournamentSize) { this.tournamentSize = tournamentSize; }
        public int getPenaltyHardConstraint() { return penaltyHardConstraint; }
        public void setPenaltyHardConstraint(int penaltyHardConstraint) { this.penaltyHardConstraint = penaltyHardConstraint; }
        public int getPenaltyUnderStaffing() { return penaltyUnderStaffing; }
        public void setPenaltyUnderStaffing(int penaltyUnderStaffing) { this.penaltyUnderStaffing = penaltyUnderStaffing; }
        public int getPenaltyOverStaffing() { return penaltyOverStaffing; }
        public void setPenaltyOverStaffing(int penaltyOverStaffing) { this.penaltyOverStaffing = penaltyOverStaffing; }
        public int getPenaltyForbiddenSuccession() { return penaltyForbiddenSuccession; }
        public void setPenaltyForbiddenSuccession(int penaltyForbiddenSuccession) { this.penaltyForbiddenSuccession = penaltyForbiddenSuccession; }
        public int getCurrentGeneration() { return currentGeneration; }
        public void setCurrentGeneration(int currentGeneration) { this.currentGeneration = currentGeneration; }
    }
} 