// dto/ShiftDTO.java
package com.simpleroster.routegenerator.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Data Transfer Object (DTO) representing a shift assignment in the system.
 * This class contains information about a specific shift including the assigned employee,
 * task, and timing details.
 */
@Data
public class ShiftDTO {
    /** Unique identifier for the shift */
    private Long id;
    
    /** ID of the employee assigned to this shift */
    private Long employeeId;
    
    /** Name of the employee assigned to this shift */
    private String employeeName;
    
    /** ID of the task associated with this shift */
    private Long taskId;
    
    /** Name of the task associated with this shift */
    private String taskName;
    
    /** Date when the shift occurs */
    private LocalDate shiftDate;
    
    /** Start time of the shift */
    private LocalTime startTime;
    
    /** End time of the shift */
    private LocalTime endTime;
}