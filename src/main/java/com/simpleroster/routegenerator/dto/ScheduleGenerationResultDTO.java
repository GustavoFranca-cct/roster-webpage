package com.simpleroster.routegenerator.dto;

import lombok.Data;
import java.util.List;

/**
 * DTO for the result of schedule generation, including both the generated shifts and any explanations for empty results.
 */
@Data
public class ScheduleGenerationResultDTO {
    private List<ShiftDTO> shifts;
    private List<String> explanations;

    public ScheduleGenerationResultDTO(List<ShiftDTO> shifts, List<String> explanations) {
        this.shifts = shifts;
        this.explanations = explanations;
    }
}
