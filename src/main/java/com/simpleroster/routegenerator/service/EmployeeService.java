// service/EmployeeService.java
package com.simpleroster.routegenerator.service;

import com.simpleroster.routegenerator.dto.EmployeeDTO;
import com.simpleroster.routegenerator.entity.Employee;
import com.simpleroster.routegenerator.entity.Skill;
import com.simpleroster.routegenerator.repository.EmployeeRepository;
import com.simpleroster.routegenerator.repository.SkillRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*; // Import POI classes
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory

import java.io.InputStream;
import java.util.ArrayList; // Import ArrayList
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Iterator; // Import Iterator

/**
 * Service layer for managing Employee entities.
 * Handles CRUD operations, activation/deactivation, skill management,
 * and importing employees from Excel files.
 */
@Service
@RequiredArgsConstructor // Lombok: Auto-creates constructor for final fields
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class); // Add logger
    private final EmployeeRepository employeeRepository;
    private final SkillRepository skillRepository;


    /**
     * Retrieves a list of all active employees.
     *
     * @return List of EmployeeDTOs for active employees.
     */
    // Return only active employees by default
    @Transactional(readOnly = true)
    public List<EmployeeDTO> getAllEmployees() {
        log.info("Fetching all active employees.");
        return employeeRepository.findAllByIsActive(true).stream() // Changed to use new repository method
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of all employees, including inactive ones.
     *
     * @return List of EmployeeDTOs for all employees.
     */
    // Method to get ALL employees (including inactive)
    @Transactional(readOnly = true)
    public List<EmployeeDTO> getAllEmployeesIncludingInactive() {
        log.info("Fetching all employees (including inactive).");
        return employeeRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single employee by their ID.
     *
     * @param id The ID of the employee.
     * @return The EmployeeDTO.
     * @throws EntityNotFoundException if no employee is found with the given ID.
     */
    @Transactional(readOnly = true)
    public EmployeeDTO getEmployeeById(Long id) {
        log.info("Fetching employee by ID: {}", id);
        return employeeRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> {
                    log.warn("Employee not found with ID: {}", id);
                    return new EntityNotFoundException("Employee not found with id: " + id);
                });
    }

    /**
     * Creates a new employee.
     * Checks for existing employee with the same name.
     *
     * @param employeeDTO DTO containing the new employee's details.
     * @return The created EmployeeDTO.
     * @throws IllegalArgumentException if an employee with the same name already exists.
     */
    @Transactional
    public EmployeeDTO createEmployee(EmployeeDTO employeeDTO) {
        log.info("Attempting to create employee: {}", employeeDTO.getName());
        employeeRepository.findByName(employeeDTO.getName()).ifPresent(e -> {
            log.warn("Employee creation failed: Name '{}' already exists.", employeeDTO.getName());
            throw new IllegalArgumentException("Employee with name '" + employeeDTO.getName() + "' already exists.");
        });

        Employee employee = mapToEntity(employeeDTO);
        employee.setActive(true); // New employees are active by default
        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employee '{}' created successfully with ID: {}", savedEmployee.getName(), savedEmployee.getId());
        return mapToDTO(savedEmployee);
    }

    /**
     * Updates an existing employee.
     * Allows updating basic details, active status, soft constraint parameters, and skills.
     * Checks for name conflicts if the name is changed.
     *
     * @param id          The ID of the employee to update.
     * @param employeeDTO DTO containing the updated employee details.
     * @return The updated EmployeeDTO.
     * @throws EntityNotFoundException if the employee is not found.
     * @throws IllegalArgumentException if the new name conflicts with another existing employee.
     */
    @Transactional
    public EmployeeDTO updateEmployee(Long id, EmployeeDTO employeeDTO) {
        log.info("Attempting to update employee with ID: {}", id);
        Employee existingEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Update failed: Employee not found with ID: {}", id);
                    return new EntityNotFoundException("Employee not found with id: " + id);
                });

        // Check if name is being changed and if the new name already exists
        if (!existingEmployee.getName().equals(employeeDTO.getName())) {
            log.debug("Employee name change detected for ID {}. Checking for conflicts with new name: {}", id, employeeDTO.getName());
            employeeRepository.findByName(employeeDTO.getName()).ifPresent(e -> {
                if (!e.getId().equals(id)) { // Ensure it's not the same employee
                    log.warn("Update failed for ID {}: New name '{}' conflicts with existing employee ID {}.", id, employeeDTO.getName(), e.getId());
                    throw new IllegalArgumentException("Employee with name '" + employeeDTO.getName() + "' already exists.");
                }
            });
        }

        // Update basic fields
        log.trace("Updating basic fields for employee ID: {}", id);
        existingEmployee.setName(employeeDTO.getName());
        existingEmployee.setContractHours(employeeDTO.getContractHours());
        existingEmployee.setAvailability(employeeDTO.getAvailability());
        existingEmployee.setPreferences(employeeDTO.getPreferences());
        existingEmployee.setActive(employeeDTO.isActive());

        // Update soft constraint parameters
        log.trace("Updating soft constraint parameters for employee ID: {}", id);
        existingEmployee.setMaxConsecutiveDays(employeeDTO.getMaxConsecutiveDays() != null ? employeeDTO.getMaxConsecutiveDays() : existingEmployee.getMaxConsecutiveDays());
        existingEmployee.setMinConsecutiveDays(employeeDTO.getMinConsecutiveDays() != null ? employeeDTO.getMinConsecutiveDays() : existingEmployee.getMinConsecutiveDays());
        existingEmployee.setMaxWeekends(employeeDTO.getMaxWeekends() != null ? employeeDTO.getMaxWeekends() : existingEmployee.getMaxWeekends());
        existingEmployee.setMaxTotalHours(employeeDTO.getMaxTotalHours()); // Allow null
        existingEmployee.setMinTotalHours(employeeDTO.getMinTotalHours() != null ? employeeDTO.getMinTotalHours() : existingEmployee.getMinTotalHours());
        existingEmployee.setConsecutiveDayPenaltyWeight(employeeDTO.getConsecutiveDayPenaltyWeight() != null ? employeeDTO.getConsecutiveDayPenaltyWeight() : existingEmployee.getConsecutiveDayPenaltyWeight());
        existingEmployee.setWeekendPenaltyWeight(employeeDTO.getWeekendPenaltyWeight() != null ? employeeDTO.getWeekendPenaltyWeight() : existingEmployee.getWeekendPenaltyWeight());
        existingEmployee.setTotalHoursPenaltyWeight(employeeDTO.getTotalHoursPenaltyWeight() != null ? employeeDTO.getTotalHoursPenaltyWeight() : existingEmployee.getTotalHoursPenaltyWeight());

        // Update skills
        log.trace("Updating skills for employee ID: {}. Desired skills: {}", id, employeeDTO.getSkills());
        Set<Skill> skillsToAssign = skillRepository.findByNameIn(employeeDTO.getSkills());
        log.trace("Found {} matching skill entities in database.", skillsToAssign.size());

        // Efficiently update skills using helper methods for bidirectional consistency
        Set<Skill> currentSkills = new HashSet<>(existingEmployee.getSkills());
        // Remove skills that are no longer assigned
        currentSkills.stream()
                .filter(skill -> !skillsToAssign.contains(skill))
                .forEach(skillToRemove -> {
                    log.trace("Removing skill '{}' from employee {}", skillToRemove.getName(), id);
                    existingEmployee.removeSkill(skillToRemove);
                });
        // Add skills that are newly assigned
        skillsToAssign.stream()
                .filter(skill -> !currentSkills.contains(skill))
                .forEach(skillToAdd -> {
                    log.trace("Adding skill '{}' to employee {}", skillToAdd.getName(), id);
                    existingEmployee.addSkill(skillToAdd);
                });

        Employee updatedEmployee = employeeRepository.save(existingEmployee);
        log.info("Employee {} updated successfully.", id);
        return mapToDTO(updatedEmployee);
    }

    /**
     * Deactivates an employee by setting their active status to false.
     *
     * @param id The ID of the employee to deactivate.
     * @throws EntityNotFoundException if the employee is not found.
     */
    @Transactional
    public void deactivateEmployee(Long id) {
        log.info("Attempting to deactivate employee with ID: {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Deactivation failed: Employee not found with ID: {}", id);
                    return new EntityNotFoundException("Employee not found with id: " + id);
                });
        if (!employee.isActive()) {
            log.warn("Employee {} is already inactive.", id);
            return; // Or throw exception?
        }
        employee.setActive(false);
        employeeRepository.save(employee);
        log.info("Employee {} deactivated successfully.", id);
        // Note: Consider implications - should future shifts be removed?
        // For now, just marking as inactive.
    }

    /**
     * Activates an employee by setting their active status to true.
     *
     * @param id The ID of the employee to activate.
     * @throws EntityNotFoundException if the employee is not found.
     */
    @Transactional
    public void activateEmployee(Long id) {
        log.info("Attempting to activate employee with ID: {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Activation failed: Employee not found with ID: {}", id);
                    return new EntityNotFoundException("Employee not found with id: " + id);
                });
        if (employee.isActive()) {
             log.warn("Employee {} is already active.", id);
             return; // Or throw exception?
        }
        employee.setActive(true);
        employeeRepository.save(employee);
        log.info("Employee {} activated successfully.", id);
    }

    // --- Mappers (Updated for Skills & Constraints) ---

    /**
     * Maps an Employee entity to an EmployeeDTO.
     * Includes mapping of skills and soft constraint parameters.
     *
     * @param employee The Employee entity.
     * @return The corresponding EmployeeDTO.
     */
    private EmployeeDTO mapToDTO(Employee employee) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(employee.getId());
        dto.setName(employee.getName());
        dto.setContractHours(employee.getContractHours());
        dto.setAvailability(employee.getAvailability());
        dto.setPreferences(employee.getPreferences());
        dto.setActive(employee.isActive());
        // Map skills to names
        if (employee.getSkills() != null) {
            dto.setSkills(employee.getSkills().stream()
                                  .map(Skill::getName)
                                  .collect(Collectors.toSet()));
        } else {
            dto.setSkills(new HashSet<>()); // Ensure it's not null
        }

        // Map soft constraint parameters
        dto.setMaxConsecutiveDays(employee.getMaxConsecutiveDays());
        dto.setMinConsecutiveDays(employee.getMinConsecutiveDays());
        dto.setMaxWeekends(employee.getMaxWeekends());
        dto.setMaxTotalHours(employee.getMaxTotalHours());
        dto.setMinTotalHours(employee.getMinTotalHours());
        dto.setConsecutiveDayPenaltyWeight(employee.getConsecutiveDayPenaltyWeight());
        dto.setWeekendPenaltyWeight(employee.getWeekendPenaltyWeight());
        dto.setTotalHoursPenaltyWeight(employee.getTotalHoursPenaltyWeight());

        return dto;
    }

    /**
     * Maps an EmployeeDTO to an Employee entity.
     * Handles mapping of basic fields, soft constraints, and resolving skill names to Skill entities.
     * Skills not found in the database are ignored.
     *
     * @param dto The EmployeeDTO.
     * @return The corresponding Employee entity (not yet persisted).
     */
    private Employee mapToEntity(EmployeeDTO dto) {
        Employee employee = new Employee();
        // ID is handled elsewhere
        employee.setName(dto.getName());
        employee.setContractHours(dto.getContractHours());
        employee.setAvailability(dto.getAvailability());
        employee.setPreferences(dto.getPreferences());
        employee.setActive(dto.isActive());

        // Map soft constraint parameters (use defaults from entity if DTO field is null)
        employee.setMaxConsecutiveDays(dto.getMaxConsecutiveDays() != null ? dto.getMaxConsecutiveDays() : new Employee().getMaxConsecutiveDays());
        employee.setMinConsecutiveDays(dto.getMinConsecutiveDays() != null ? dto.getMinConsecutiveDays() : new Employee().getMinConsecutiveDays());
        employee.setMaxWeekends(dto.getMaxWeekends() != null ? dto.getMaxWeekends() : new Employee().getMaxWeekends());
        employee.setMaxTotalHours(dto.getMaxTotalHours()); // Allow null
        employee.setMinTotalHours(dto.getMinTotalHours() != null ? dto.getMinTotalHours() : new Employee().getMinTotalHours());
        employee.setConsecutiveDayPenaltyWeight(dto.getConsecutiveDayPenaltyWeight() != null ? dto.getConsecutiveDayPenaltyWeight() : new Employee().getConsecutiveDayPenaltyWeight());
        employee.setWeekendPenaltyWeight(dto.getWeekendPenaltyWeight() != null ? dto.getWeekendPenaltyWeight() : new Employee().getWeekendPenaltyWeight());
        employee.setTotalHoursPenaltyWeight(dto.getTotalHoursPenaltyWeight() != null ? dto.getTotalHoursPenaltyWeight() : new Employee().getTotalHoursPenaltyWeight());

        // Map skill names back to Skill entities
        if (dto.getSkills() != null && !dto.getSkills().isEmpty()) {
            Set<Skill> skills = skillRepository.findByNameIn(dto.getSkills());
            // Important: Handle case where a skill name in DTO doesn't exist?
            // For now, we only add skills found in the repository.
            // Could create new skills here if desired, or throw error.
            skills.forEach(employee::addSkill); // Use helper method
        }
        // employee.setSkills(skills); // Don't just set, use addSkill
        return employee;
    }

    // --- Excel Import Logic ---

    // Define column indexes for clarity and maintainability
    // Adjust these constants if your Excel file format changes.
    private static final int COLUMN_INDEX_AVAILABILITY = 0;
    private static final int COLUMN_INDEX_CONTRACT_HOURS = 1;
    private static final int COLUMN_INDEX_NAME = 2;
    private static final int COLUMN_INDEX_PREFERENCES = 3;
    private static final int COLUMN_INDEX_MIN_TOTAL_HOURS = 4;
    private static final int COLUMN_INDEX_MAX_TOTAL_HOURS = 5;
    private static final int COLUMN_INDEX_MAX_CONSECUTIVE_DAYS = 6;
    private static final int COLUMN_INDEX_MIN_CONSECUTIVE_DAYS = 7;
    private static final int COLUMN_INDEX_MAX_WEEKENDS = 8;
    private static final int COLUMN_INDEX_TOTAL_HOURS_PENALTY_WEIGHT = 9;
    private static final int COLUMN_INDEX_WEEKEND_PENALTY_WEIGHT = 10;
    private static final int COLUMN_INDEX_CONSECUTIVE_DAY_PANALTY_WEIGHT = 11;
    private static final int COLUMN_INDEX_SKILLS = 12;


    /**
     * Imports employee data from an Excel (.xlsx) file.
     * Reads data row by row, validates, maps to EmployeeDTO, and attempts to create/update.
     * Collects errors encountered during the process.
     *
     * @param file The multipart file containing the Excel data.
     * @return The number of employees successfully imported/updated.
     * @throws IllegalArgumentException if the file is empty, has no sheet, or a major parsing error occurs.
     */
    @Transactional // Important: Wrap import in a transaction for atomicity
    public int importEmployeesFromExcel(MultipartFile file) {
        log.info("Starting Excel import process for file: {}", file.getOriginalFilename());
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int skippedEmptyRows = 0;
        int processedRows = 0;

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) { // Use XSSFWorkbook for .xlsx

            Sheet sheet = workbook.getSheetAt(0); // Assuming data is on the first sheet
            if (sheet == null) {
                throw new IllegalArgumentException("Excel file is empty or sheet not found.");
            }
            Iterator<Row> rowIterator = sheet.iterator();

            // Skip header row
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            int rowNum = 1; // Start after header
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                rowNum++;
                processedRows++;
                EmployeeDTO dto = new EmployeeDTO();
                boolean rowHasMeaningfulData = false;

                try {
                    // --- Extract Data --- 
                    String name = getCellValueAsString(row.getCell(COLUMN_INDEX_NAME));
                    // Basic check: Skip row entirely if the name cell is blank
                    if (name == null || name.trim().isEmpty()) {
                        // Check if the entire row is likely blank
                        if (!isRowEffectivelyBlank(row, COLUMN_INDEX_AVAILABILITY)) {
                             log.warn("Skipping row {}: Name is empty but other data might exist.", rowNum);
                             errors.add("Row " + rowNum + ": Name is missing.");
                        } else {
                            skippedEmptyRows++;
                        }
                        continue; // Skip processing this row
                    }
                    dto.setName(name.trim());
                    rowHasMeaningfulData = true; 

                    // Contract Hours (Handle potential non-numeric values)
                    try {
                        double contractHoursDouble = getCellValueAsNumeric(row.getCell(COLUMN_INDEX_CONTRACT_HOURS));
                         if (contractHoursDouble < 0) {
                             log.warn("Row {}: Negative contract hours ({}) found for {}. Setting to 0.", rowNum, contractHoursDouble, dto.getName());
                             dto.setContractHours(0);
                         } else {
                            dto.setContractHours((int) contractHoursDouble);
                         }
                    } catch (NumberFormatException e) {
                         log.warn("Row {}: Invalid numeric value for Contract Hours for {}. Setting to 0.", rowNum, dto.getName());
                         errors.add("Row " + rowNum + " ('" + dto.getName() + "'): Invalid Contract Hours format (must be a number).");
                         dto.setContractHours(0); // Default on error
                    }

                    // Availability & Preferences (Stored as strings)
                    dto.setAvailability(getCellValueAsString(row.getCell(COLUMN_INDEX_AVAILABILITY), "")); // Default to empty string if null
                    dto.setPreferences(getCellValueAsString(row.getCell(COLUMN_INDEX_PREFERENCES), "")); // Default to empty string if null
                    dto.setMinTotalHours(getCellValueAsNumeric(row.getCell(COLUMN_INDEX_MIN_TOTAL_HOURS), 20));
                    dto.setMaxTotalHours(getCellValueAsNumeric(row.getCell(COLUMN_INDEX_MAX_TOTAL_HOURS), 40));
                    dto.setMaxConsecutiveDays(getCellValueAsNumeric(row.getCell(COLUMN_INDEX_MAX_CONSECUTIVE_DAYS)));
                    dto.setMinConsecutiveDays(getCellValueAsNumeric(row.getCell(COLUMN_INDEX_MIN_CONSECUTIVE_DAYS)));
                    dto.setMaxWeekends(getCellValueAsNumeric(row.getCell(COLUMN_INDEX_MAX_WEEKENDS), 5));
                    dto.setTotalHoursPenaltyWeight(getCellValueAsNumeric(row.getCell(COLUMN_INDEX_TOTAL_HOURS_PENALTY_WEIGHT)));
                    dto.setWeekendPenaltyWeight(getCellValueAsNumeric(row.getCell(COLUMN_INDEX_WEEKEND_PENALTY_WEIGHT)));
                    dto.setConsecutiveDayPenaltyWeight(getCellValueAsNumeric(row.getCell(COLUMN_INDEX_CONSECUTIVE_DAY_PANALTY_WEIGHT)));

                    // Skills (Comma-separated string)
                    String skillsString = getCellValueAsString(row.getCell(COLUMN_INDEX_SKILLS));
                    if (skillsString != null && !skillsString.trim().isEmpty()) {
                        Set<String> skills = Set.of(skillsString.split(","))
                                                .stream()
                                                .map(String::trim)
                                                .filter(s -> !s.isEmpty())
                                                .collect(Collectors.toSet());
                        dto.setSkills(skills);
                    } else {
                        dto.setSkills(new HashSet<>());
                    }

                    // --- Set Defaults & Attempt Creation --- 
                    dto.setActive(true); // Default imported employees to active
                    // Set defaults for new constraint fields if not provided in Excel (optional)
                    // These will be picked up by mapToEntity if null in DTO
                   

                    // Attempt to create the employee using existing service method
                    try {
                        createEmployee(dto); // Reuses validation (like duplicate name check) and skill lookup
                        successCount++;
                        // log.info("Successfully imported employee from row {}: {}", rowNum, dto.getName()); // Reduce log verbosity
                    } catch (IllegalArgumentException e) {
                        // Catch errors from createEmployee (e.g., duplicate name)
                        log.error("Skipping row {}: Validation error during import for '{}' - {}", rowNum, dto.getName(), e.getMessage());
                        errors.add("Row " + rowNum + " ('" + dto.getName() + "'): " + e.getMessage());
                    } catch (Exception serviceEx) {
                         // Catch unexpected errors during employee creation
                         log.error("Skipping row {}: Unexpected service error for '{}' - {}", rowNum, dto.getName(), serviceEx.getMessage(), serviceEx);
                         errors.add("Row " + rowNum + " ('" + dto.getName() + "'): Internal error saving employee.");
                    }

                } catch (Exception cellReadError) {
                    // Catch errors during cell reading/parsing within the row
                    log.error("Error processing data in row {}: {}", rowNum, cellReadError.getMessage());
                    if(rowHasMeaningfulData) { // Only report error if we started reading data
                       errors.add("Row " + rowNum + ": Error reading data - " + cellReadError.getMessage());
                    } else {
                         skippedEmptyRows++; // Row was likely blank or only had unreadable cells
                    }
                }
            } // End while loop

        } catch (Exception e) {
            log.error("Fatal error during Excel import processing: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process Excel file. Ensure it is a valid .xlsx file and data is in the correct format. Error: " + e.getMessage(), e);
        }

        log.info("Excel import finished. Processed Rows: {}, Success: {}, Errors/Skipped: {}, Blank Rows Skipped: {}",
                 processedRows, successCount, errors.size(), skippedEmptyRows);

        // Throw exception if any errors occurred to provide feedback to user
        if (!errors.isEmpty()) {
            String combinedErrors = errors.stream().limit(15).collect(Collectors.joining("; ")); // Limit shown errors
            if(errors.size() > 15) combinedErrors += "; ... (" + (errors.size() - 15) + " more errors)";
            // Use a more specific exception or wrap the message
            throw new IllegalArgumentException("Import completed with issues. Please check data. Errors: " + combinedErrors);
        }

        return successCount;
    }

    /**
     * Checks if a row in the Excel sheet is effectively blank up to a certain column.
     * Useful for skipping completely empty rows often found at the end of sheets.
     *
     * @param row The Row object.
     * @param lastColumnIndexToCheck The index of the last column to check for content.
     * @return true if all cells up to the specified index are null or blank, false otherwise.
     */
    private boolean isRowEffectivelyBlank(Row row, int lastColumnIndexToCheck) {
        if (row == null) return true;
        for (int i = 0; i <= lastColumnIndexToCheck; i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                 String value = getCellValueAsString(cell);
                 if (value != null && !value.trim().isEmpty()) {
                     return false; // Found non-blank cell
                 }
            }
        }
        return true; // All cells up to the index were blank or null
    }

    /**
     * Helper method to safely get a cell's value as a String, handling nulls and different cell types.
     * Returns a default value if the cell is null or blank.
     *
     * @param cell The Cell object.
     * @param defaultValue The value to return if the cell is null or blank.
     * @return The cell value as a String or the default value.
     */
    private String getCellValueAsString(Cell cell, String defaultValue) {
        String value = getCellValueAsString(cell);
        return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
    }

    /**
     * Helper method to safely get a cell's value as a String, handling nulls and different cell types.
     *
     * @param cell The Cell object.
     * @return The cell value as a String, or null if the cell is null or blank.
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // Handle numeric values appropriately (e.g., avoid scientific notation for IDs)
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString(); // Or format as needed
                } else {
                    // Format as plain number string to avoid issues
                     DataFormatter dataFormatter = new DataFormatter();
                     return dataFormatter.formatCellValue(cell);
                    // return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // Evaluate formula, then get the result as string
                try {
                     DataFormatter dataFormatter = new DataFormatter();
                     return dataFormatter.formatCellValue(cell, cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator());
                    // CellValue cellValue = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator().evaluate(cell);
                    // return getCellValueAsStringFromEvaluatedType(cellValue);
                 } catch (Exception e) {
                    log.warn("Could not evaluate formula in cell {}: {}", cell.getAddress(), e.getMessage());
                     return null; // Or return error indicator
                 }
            case BLANK:
                return null;
            default:
                return null;
        }
    }

    /**
     * Helper method to safely get a cell's value as an Integer, handling nulls, blanks, and non-numeric types.
     * Returns a default value if the cell is invalid or cannot be parsed as an integer.
     *
     * @param cell         The Cell object.
     * @param defaultValue The default value to return on error or blank cell.
     * @return The cell value as an Integer or the default value.
     */
    private int getCellValueAsNumeric(Cell cell, int defaultValue) {
        if (cell == null) return defaultValue;
        return getCellValueAsNumeric(cell);
    }

    /**
     * Helper method to safely get a cell's value as an Integer, handling errors.
     * Throws NumberFormatException if the cell is not a valid numeric type or cannot be parsed.
     *
     * @param cell The Cell object.
     * @return The cell value as an int.
     * @throws NumberFormatException if conversion fails.
     */
    private int getCellValueAsNumeric(Cell cell) {
        if (cell == null) {
            throw new NumberFormatException("Cell is null, cannot get numeric value.");
        }
        CellType cellType = cell.getCellType();
         if (cellType == CellType.FORMULA) { // Handle formula cells first
             cellType = cell.getCachedFormulaResultType(); // Get the type of the formula result
         }

        switch (cellType) {
            case NUMERIC:
                Double doubleValue = cell.getNumericCellValue();
                int intValue = doubleValue.intValue();
                return intValue;
            case STRING:
                try {
                    String stringValue = cell.getStringCellValue().trim();
                    if (stringValue.isEmpty()) {
                         throw new NumberFormatException("Cannot parse empty string as number.");
                    }
                    return Integer.parseInt(stringValue);
                } catch (NumberFormatException e) {
                    // Re-throw with more context
                     throw new NumberFormatException("Invalid number format in cell " + cell.getAddress() + ": '" + cell.getStringCellValue() + "'");
                }
            // case FORMULA: // Handled above by getting cached result type
            //      try {
            //          // Attempt to evaluate if needed, but prefer cached value
            //          return cell.getNumericCellValue();
            //      } catch (Exception e) {
            //          log.warn("Could not get numeric value from formula cell {}: {}", cell.getAddress(), e.getMessage());
            //          throw new NumberFormatException("Could not evaluate formula in cell " + cell.getAddress() + " to a number.");
            //      }
            default:
                 throw new NumberFormatException("Cell " + cell.getAddress() + " is not a numeric type (Type: " + cellType + ")");
        }
    }
}
