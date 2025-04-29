package com.simpleroster.routegenerator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing a skill in the system.
 * This class contains the basic information about a skill that can be assigned to employees
 * and required for tasks.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillDTO {
    /** Unique identifier for the skill */
    private Long id;
    
    /** Name of the skill */
    private String name;
    // Add description etc. if needed later
} 