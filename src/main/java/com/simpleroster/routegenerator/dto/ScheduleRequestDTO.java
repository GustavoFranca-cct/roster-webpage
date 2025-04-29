// dto/ScheduleRequestDTO.java
package com.simpleroster.routegenerator.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) representing a request to generate a schedule.
 * This class contains the time period and business hours parameters needed
 * for schedule generation.
 */
@Data
public class ScheduleRequestDTO {
    /** Start date of the scheduling period */
    private LocalDate startDate; // e.g., "2023-11-20"
    
    /** End date of the scheduling period */
    private LocalDate endDate;   // e.g., "2023-11-26"

    // Added Business Hours (as strings like "HH:MM")
    /** Business start time in 24-hour format (HH:MM) */
    private String businessStartTime; // e.g., "09:00"
    
    /** Business end time in 24-hour format (HH:MM) */
    private String businessEndTime;   // e.g., "17:00"
}