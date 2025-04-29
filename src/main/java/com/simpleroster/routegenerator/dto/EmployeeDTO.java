// dto/EmployeeDTO.java
package com.simpleroster.routegenerator.dto;

import lombok.Data;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object (DTO) representing an employee in the system.
 * This class contains all employee information including their basic details,
 * work preferences, constraints, and penalty weights for scheduling optimization.
 */
@Data // Use DTOs to decouple API from internal Entity structure
public class EmployeeDTO {
    /** Unique identifier for the employee */
    private Long id;
    
    /** Full name of the employee */
    private String name;
    
    /** Contracted hours per week for the employee */
    private int contractHours;
    
    /** Employee's availability pattern stored as a string */
    private String availability; // Keep as string for simplicity matching entity
    
    /** Employee's work preferences stored as a string */
    private String preferences;

    /** Flag indicating if the employee is currently active in the system */
    @JsonProperty("isActive") // Ensure JSON field name matches frontend expectation
    private boolean isActive;

    /** Set of skill identifiers possessed by the employee */
    private Set<String> skills = new HashSet<>();

    /** Maximum number of consecutive days the employee can work */
    private Integer maxConsecutiveDays;
    
    /** Minimum number of consecutive days the employee should work */
    private Integer minConsecutiveDays;
    
    /** Maximum number of weekends the employee can work in a period */
    private Integer maxWeekends;
    
    /** Maximum total hours the employee can work in a period */
    private Integer maxTotalHours;
    
    /** Minimum total hours the employee should work in a period */
    private Integer minTotalHours;
    
    /** Weight factor for penalty calculations related to consecutive days constraints */
    private Integer consecutiveDayPenaltyWeight;
    
    /** Weight factor for penalty calculations related to weekend work constraints */
    private Integer weekendPenaltyWeight;
    
    /** Weight factor for penalty calculations related to total hours constraints */
    private Integer totalHoursPenaltyWeight;
    // Add other fields as needed
}