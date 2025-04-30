// controller/ApiController.java
package com.simpleroster.routegenerator.controller;

import com.simpleroster.routegenerator.dto.*; // Using wildcard import for multiple DTOs
import com.simpleroster.routegenerator.service.*; // Using wildcard import for multiple Services
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Main REST Controller for handling core application API requests related to:
 * - Employees (CRUD, activate/deactivate, import)
 * - Schedules (Generate, Retrieve, Delete)
 * - Tasks (CRUD)
 * - Dashboard (Stats, Alerts)
 * - Configuration Settings (Retrieve, Update)
 *
 * Base path for all endpoints in this controller is /api.
 */
@RestController
@RequestMapping("/api") // Base path for all API endpoints
@RequiredArgsConstructor
@CrossOrigin // TODO: Configure CORS more restrictively for production environments!
public class ApiController {
    private final EmployeeService employeeService;
    private final ScheduleService scheduleService;
    private final TaskService taskService;
    private final DashboardService dashboardService;
    private final ConfigurationService configurationService;
    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    // --- Employee Endpoints ---

    /**
     * Retrieves a list of all employees.
     * By default, only active employees are returned.
     *
     * @param includeInactive If true, includes inactive employees in the list. Defaults to false.
     * @return A list of EmployeeDTO objects.
     */
    @GetMapping("/employees")
    public List<EmployeeDTO> getAllEmployees(
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive) {
        log.info("GET /employees requested (includeInactive={})", includeInactive);
        if (includeInactive) {
            return employeeService.getAllEmployeesIncludingInactive();
        } else {
            return employeeService.getAllEmployees(); // Default: only active
        }
    }

    /**
     * Retrieves a specific employee by their ID.
     *
     * @param id The ID of the employee to retrieve.
     * @return ResponseEntity containing the EmployeeDTO if found (200 OK), or 404 Not Found.
     */
    @GetMapping("/employees/{id}")
    public ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable Long id) {
        log.info("GET /employees/{} requested", id);
        try {
            return ResponseEntity.ok(employeeService.getEmployeeById(id));
        } catch (EntityNotFoundException e) {
            log.warn("Employee not found for ID: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    /**
     * Creates a new employee.
     *
     * @param employeeDTO DTO containing the details of the employee to create.
     * @return ResponseEntity containing the created EmployeeDTO (201 Created), or 400 Bad Request if input is invalid.
     */
    @PostMapping("/employees")
    public ResponseEntity<EmployeeDTO> createEmployee(@RequestBody EmployeeDTO employeeDTO) {
        log.info("POST /employees requested for employee: {}", employeeDTO.getName());
        try {
            EmployeeDTO createdEmployee = employeeService.createEmployee(employeeDTO);
            log.info("Employee created successfully with ID: {}", createdEmployee.getId());
            return new ResponseEntity<>(createdEmployee, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create employee: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * Updates an existing employee's details.
     *
     * @param id          The ID of the employee to update.
     * @param employeeDTO DTO containing the updated details.
     * @return ResponseEntity containing the updated EmployeeDTO if successful (200 OK),
     *         404 Not Found if the employee doesn't exist, or 400 Bad Request if input is invalid.
     */
    @PutMapping("/employees/{id}")
    public ResponseEntity<EmployeeDTO> updateEmployee(@PathVariable Long id, @RequestBody EmployeeDTO employeeDTO) {
         log.info("PUT /employees/{} requested", id);
        try {
            EmployeeDTO updatedEmployee = employeeService.updateEmployee(id, employeeDTO);
            log.info("Employee {} updated successfully.", id);
            return ResponseEntity.ok(updatedEmployee);
        } catch (EntityNotFoundException e) {
             log.warn("Employee not found for update, ID: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) { // Handle potential validation errors from service
             log.warn("Failed to update employee {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

     /**
      * Deactivates an employee (marks them as inactive).
      *
      * @param id The ID of the employee to deactivate.
      * @return ResponseEntity with a confirmation message (200 OK) if successful, or 404 Not Found if the employee doesn't exist.
      */
    @PutMapping("/employees/{id}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateEmployee(@PathVariable Long id) {
        log.info("PUT /employees/{}/deactivate requested", id);
        try {
            employeeService.deactivateEmployee(id);
             log.info("Employee {} deactivated successfully.", id);
             Map<String, String> responseBody = Map.of("message", "Employee deactivated successfully");
            return ResponseEntity.ok(responseBody);
        } catch (EntityNotFoundException e) {
             log.warn("Employee not found for deactivation, ID: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

     /**
      * Activates an employee (marks them as active).
      *
      * @param id The ID of the employee to activate.
      * @return ResponseEntity with a confirmation message (200 OK) if successful, or 404 Not Found if the employee doesn't exist.
      */
    @PutMapping("/employees/{id}/activate")
    public ResponseEntity<Map<String, String>> activateEmployee(@PathVariable Long id) {
         log.info("PUT /employees/{}/activate requested", id);
        try {
            employeeService.activateEmployee(id);
             log.info("Employee {} activated successfully.", id);
             Map<String, String> responseBody = Map.of("message", "Employee activated successfully");
             return ResponseEntity.ok(responseBody);
        } catch (EntityNotFoundException e) {
             log.warn("Employee not found for activation, ID: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    // --- Employee Import Endpoint ---
    /**
     * Imports employees from an uploaded Excel file (.xlsx).
     * Expects a multipart/form-data request with a file part named "file".
     *
     * @param file The uploaded Excel file.
     * @return ResponseEntity indicating success (200 OK with count), bad request (400),
     *         unsupported media type (415), or internal server error (500).
     */
    @PostMapping("/employees/import")
    public ResponseEntity<?> importEmployees(@RequestParam("file") MultipartFile file) {
        log.info("POST /employees/import requested for file: {}", file.getOriginalFilename());
        if (file.isEmpty()) {
             log.warn("Employee import failed: No file selected.");
            return ResponseEntity.badRequest().body("{\"message\": \"Please select an Excel file to upload.\"}");
        }

        // Basic validation for Excel file type (can be more robust)
        String contentType = file.getContentType();
        if (contentType == null ||
            (!contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") &&
             !contentType.equals("application/vnd.ms-excel"))) {
             log.warn("Employee import failed: Invalid file type '{}'", contentType);
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                                 .body("{\"message\": \"Invalid file type. Please upload an Excel file (.xlsx).\"}");
        }

        try {
            int importedCount = employeeService.importEmployeesFromExcel(file);
            String successMessage = String.format("{\"message\": \"Successfully imported %d employees.\"}", importedCount);
             log.info("Employee import successful: {} employees imported.", importedCount);
            return ResponseEntity.ok(successMessage);

        } catch (IllegalArgumentException e) { // Catch specific validation errors from service
             log.warn("Employee import failed: {}", e.getMessage());
             // Make sure message is JSON formatted
             String errorMessage = String.format("{\"message\": \"Import failed: %s\"}", e.getMessage().replace("\"", "\\\"")); // Escape quotes
             return ResponseEntity.badRequest().body(errorMessage);
        } catch (Exception e) {
             log.error("Error during employee import: ", e); // Log the full stack trace
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("{\"message\": \"An unexpected error occurred during import.\"}");
        }
    }

    // --- Schedule Endpoints ---

    /**
     * Generates a new schedule based on the provided date range and parameters.
     * This triggers the Genetic Algorithm in ScheduleService.
     *
     * @param request DTO containing the start and end dates.
     * @return ResponseEntity containing the list of generated ShiftDTOs (200 OK),
     *         or 500 Internal Server Error if generation fails.
     */
    @PostMapping("/schedule/generate")
    public ResponseEntity<?> generateSchedule(@RequestBody ScheduleRequestDTO request) {
        log.info("POST /schedule/generate requested for range: {} to {}", request.getStartDate(), request.getEndDate());
        try {
            var result = scheduleService.generateSchedule(request);
            log.info("Schedule generation completed. Returning {} shifts and {} explanations.",
                result.getShifts() != null ? result.getShifts().size() : 0,
                result.getExplanations() != null ? result.getExplanations().size() : 0);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
             log.warn("Schedule generation failed: {}", e.getMessage());
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error generating schedule: " + e.getMessage(), e);
        } catch (Exception e) { // Catch broader exceptions during generation
            log.error("Error generating schedule for range {} to {}", request.getStartDate(), request.getEndDate(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred during schedule generation.", e);
        }
    }

    /**
     * Retrieves the existing schedule for a given date range.
     *
     * @param startDate Start date of the range (ISO DATE format: yyyy-MM-dd).
     * @param endDate   End date of the range (ISO DATE format: yyyy-MM-dd).
     * @return A list of ShiftDTO objects for the specified range.
     */
    @GetMapping("/schedule")
    public List<ShiftDTO> getSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
         log.info("GET /schedule requested for range: {} to {}", startDate, endDate);
         List<ShiftDTO> schedule = scheduleService.getSchedule(startDate, endDate);
         log.info("Returning {} shifts for the requested range.", schedule.size());
        return schedule;
    }

    /**
     * Deletes all shifts within a specified date range.
     *
     * @param startDate Start date of the range (ISO DATE format: yyyy-MM-dd).
     * @param endDate   End date of the range (ISO DATE format: yyyy-MM-dd).
     * @return ResponseEntity with 204 No Content if successful, or 500 Internal Server Error on failure.
     */
    @DeleteMapping("/schedule")
    public ResponseEntity<Void> deleteSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("DELETE /schedule requested for range: {} to {}", startDate, endDate);
        try {
            scheduleService.deleteSchedule(startDate, endDate);
            log.info("Successfully deleted shifts for the range.");
            return ResponseEntity.noContent().build(); // HTTP 204 No Content on success
        } catch (Exception e) {
            log.error("Error deleting schedule for range {} to {}", startDate, endDate, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting schedule", e);
        }
    }

    // --- Task Endpoints ---

    /**
     * Retrieves a list of all tasks.
     *
     * @return A list of TaskDTO objects.
     */
    @GetMapping("/tasks")
    public List<TaskDTO> getAllTasks() {
         log.info("GET /tasks requested.");
         List<TaskDTO> tasks = taskService.getAllTasks();
         log.info("Returning {} tasks.", tasks.size());
        return tasks;
    }

    /**
     * Creates a new task.
     *
     * @param taskDTO DTO containing the details of the task to create.
     * @return ResponseEntity containing the created TaskDTO (201 Created), or 400 Bad Request if input is invalid.
     */
    @PostMapping("/tasks")
    public ResponseEntity<TaskDTO> createTask(@RequestBody TaskDTO taskDTO) {
        log.info("POST /tasks requested for task: {}", taskDTO.getName());
        try {
            TaskDTO createdTask = taskService.createTask(taskDTO);
             log.info("Task created successfully with ID: {}", createdTask.getId());
            return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
             log.warn("Failed to create task: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

     /**
      * Updates an existing task.
      *
      * @param id      The ID of the task to update.
      * @param taskDTO DTO containing the updated details.
      * @return ResponseEntity containing the updated TaskDTO if successful (200 OK),
      *         404 Not Found if the task doesn't exist, or 400 Bad Request if input is invalid.
      */
    @PutMapping("/tasks/{id}")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable Long id, @RequestBody TaskDTO taskDTO) {
         log.info("PUT /tasks/{} requested", id);
        try {
            TaskDTO updatedTask = taskService.updateTask(id, taskDTO);
             log.info("Task {} updated successfully.", id);
            return ResponseEntity.ok(updatedTask);
        } catch (jakarta.persistence.EntityNotFoundException e) {
             log.warn("Task not found for update, ID: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
             log.warn("Failed to update task {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * Deletes a task by its ID.
     *
     * @param id The ID of the task to delete.
     * @return ResponseEntity with 204 No Content if successful, 404 Not Found if the task doesn't exist,
     *         or 500 Internal Server Error on failure (e.g., dependencies exist).
     */
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        log.info("DELETE /tasks/{} requested", id);
        try {
            taskService.deleteTask(id);
            log.info("Task {} deleted successfully.", id);
            return ResponseEntity.noContent().build(); // HTTP 204
        } catch (jakarta.persistence.EntityNotFoundException e) {
             log.warn("Task not found for deletion, ID: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) { // Catch potential dependency issues etc.
            log.error("Error deleting task {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting task. It might be associated with existing shifts or employee skills.", e);
        }
    }

    // --- Dashboard Endpoints ---

    /**
     * Retrieves general statistics for the dashboard.
     *
     * @return A DashboardStatsDTO containing various stats.
     */
    @GetMapping("/dashboard/stats")
    public DashboardStatsDTO getDashboardStats() {
        log.info("GET /dashboard/stats requested.");
        return dashboardService.getDashboardStats();
    }

    /**
     * Retrieves current alerts or notifications for the dashboard.
     * (Actual alert logic might need refinement based on requirements).
     *
     * @return A list of AlertDTO objects.
     */
    @GetMapping("/dashboard/alerts")
    public List<AlertDTO> getDashboardAlerts() {
         log.info("GET /dashboard/alerts requested.");
        return dashboardService.getDashboardAlerts();
    }

    // --- Configuration Settings Endpoints ---

    /**
     * Retrieves all configuration settings stored in the database.
     *
     * @return ResponseEntity containing a map of all settings (key-value pairs).
     */
    @GetMapping("/settings")
    public ResponseEntity<Map<String, String>> getAllSettings() {
        log.info("GET /settings requested.");
        return ResponseEntity.ok(configurationService.getAllSettings());
    }

    /**
     * Retrieves configuration settings filtered by a key prefix.
     * Useful for fetching related settings (e.g., all 'ga.' settings).
     *
     * @param prefix The prefix to filter settings keys (e.g., "ga", "penalty").
     * @return ResponseEntity containing a map of matching settings.
     */
    @GetMapping("/settings/group/{prefix}")
    public ResponseEntity<Map<String, String>> getSettingsByPrefix(@PathVariable String prefix) {
        log.info("GET /settings/group/{} requested.", prefix);
        // Add a dot if prefix doesn't end with one, for consistency e.g., "ga" -> "ga."
        String keyPrefix = prefix.endsWith(".") ? prefix : prefix + ".";
        return ResponseEntity.ok(configurationService.getSettingsByPrefix(keyPrefix));
    }

    /**
     * Updates multiple configuration settings.
     * Expects a map of setting keys and their new values in the request body.
     *
     * @param settings A map where keys are setting names and values are the new setting values.
     * @return ResponseEntity with 200 OK on success, or 500 Internal Server Error on failure.
     */
    @PutMapping("/settings")
    public ResponseEntity<Void> updateSettings(@RequestBody Map<String, String> settings) {
        log.info("PUT /settings requested with {} settings.", settings.size());
        try {
            configurationService.updateSettings(settings);
            log.info("Settings updated successfully.");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error updating settings", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating settings", e);
        }
    }
}
