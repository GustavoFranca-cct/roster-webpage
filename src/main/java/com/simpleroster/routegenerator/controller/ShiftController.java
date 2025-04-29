package com.simpleroster.routegenerator.controller;

import com.simpleroster.routegenerator.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST Controller specifically for managing individual Shift entities.
 * Currently only handles deletion, but could be expanded for single shift updates.
 * Base path for all endpoints in this controller is /api/shifts.
 */
@RestController
@RequestMapping("/api/shifts") // Specific path for shifts
@RequiredArgsConstructor
@CrossOrigin // TODO: Configure CORS more restrictively for production environments!
public class ShiftController {

    private static final Logger log = LoggerFactory.getLogger(ShiftController.class);
    // Inject ScheduleService as it contains the delete logic currently
    // Consider creating a dedicated ShiftService if more shift-specific operations are added.
    private final ScheduleService scheduleService;

    /**
     * Deletes a single shift by its ID.
     *
     * @param shiftId The ID of the shift to delete.
     * @return ResponseEntity with 204 No Content on successful deletion,
     *         404 Not Found if the shift doesn't exist, or 500 Internal Server Error on failure.
     */
    @DeleteMapping("/{shiftId}")
    public ResponseEntity<Void> deleteSingleShift(@PathVariable Long shiftId) {
        log.info("DELETE /shifts/{} requested", shiftId);
        try {
            scheduleService.deleteSingleShift(shiftId);
            log.info("Shift {} deleted successfully.", shiftId);
            return ResponseEntity.noContent().build(); // HTTP 204 No Content on success
        } catch (jakarta.persistence.EntityNotFoundException e) {
             log.warn("Shift not found for deletion: ID {}", shiftId);
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error deleting shift with ID {}: {}", shiftId, e.getMessage(), e);
            // Provide a generic error message to the client
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred while deleting the shift.", e);
        }
    }

    // TODO: Add other shift-specific endpoints here if needed (e.g., PUT /api/shifts/{shiftId} to update details of a single shift)

} 