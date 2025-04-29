package com.simpleroster.routegenerator.dto;

import lombok.Data;
import java.util.HashSet;
import java.util.Set;

/**
 * Data Transfer Object (DTO) representing a task in the system.
 * This class contains all the necessary information about a task including its requirements and coverage parameters.
 */
@Data
public class TaskDTO {
    /** Unique identifier for the task */
    private Long id;
    
    /** Name of the task */
    private String name;
    
    /** Detailed description of the task */
    private String description;
    
    /** Minimum number of employees required to cover this task */
    private Integer minimumCoverage;
    
    /** Optimal number of employees for this task */
    private Integer optimalCoverage;
    
    /** Weight factor used in penalty calculations for task coverage violations */
    private Integer penaltyWeight;
    
    /** Set of skill identifiers required for this task */
    private Set<String> requiredSkills = new HashSet<>();
} 