package com.simpleroster.routegenerator.dto;

import lombok.Data;

/**
 * Data Transfer Object (DTO) containing statistics for the dashboard display.
 * This class aggregates various metrics about the workforce and scheduling status.
 */
@Data
public class DashboardStatsDTO {
    /** Number of currently active employees in the system */
    private long activeEmployees;
    
    /** Total hours scheduled for the current week */
    private double hoursThisWeek; // Using double for potential fractional hours
    
    /** Number of shifts that still need to be assigned */
    private long openShifts; // Requires logic to determine what an 'open' shift is
    
    /** Number of pending time off requests awaiting approval */
    private long pendingTimeOff; // Requires time off request system
} 