package com.simpleroster.routegenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simpleroster.routegenerator.dto.ScheduleGenerationResultDTO;
import com.simpleroster.routegenerator.dto.ScheduleRequestDTO;
import com.simpleroster.routegenerator.dto.ShiftDTO;
import com.simpleroster.routegenerator.entity.Employee;
import com.simpleroster.routegenerator.entity.Shift;
import com.simpleroster.routegenerator.entity.Skill;
import com.simpleroster.routegenerator.entity.Task;
import com.simpleroster.routegenerator.repository.EmployeeRepository;
import com.simpleroster.routegenerator.repository.ShiftRepository;
import com.simpleroster.routegenerator.repository.TaskRepository;
import com.simpleroster.routegenerator.service.ConfigurationService;
import jakarta.persistence.EntityNotFoundException; // If needed for employee lookups
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate; // Import Hibernate
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream; // Added for potential future use

/**
 * Service responsible for generating and managing employee schedules (rosters).
 * Uses a Genetic Algorithm (GA) to optimize schedules based on various constraints
 * and preferences defined for employees, tasks, and overall business rules.
 * Configuration settings for the GA and penalty weights are loaded dynamically.
 */
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);
    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;
    private final TaskRepository taskRepository;
    private final ConfigurationService configurationService;
    private final ObjectMapper objectMapper = new ObjectMapper(); // For parsing JSON preferences

    // --- Existing Constants and Definitions ---
    private static final Map<String, DayOfWeek> DAY_ABBREVIATIONS = Map.of(
            "Mon", DayOfWeek.MONDAY, "Tue", DayOfWeek.TUESDAY, "Wed", DayOfWeek.WEDNESDAY,
            "Thu", DayOfWeek.THURSDAY, "Fri", DayOfWeek.FRIDAY, "Sat", DayOfWeek.SATURDAY,
            "Sun", DayOfWeek.SUNDAY
    );
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter INPUT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final LocalTime MORNING_START = LocalTime.of(7, 0);
    private static final LocalTime MORNING_END = LocalTime.of(12, 0);
    private static final LocalTime AFTERNOON_START = LocalTime.of(12, 0);
    private static final LocalTime AFTERNOON_END = LocalTime.of(17, 0);
    private static final LocalTime EVENING_START = LocalTime.of(17, 0);
    private static final LocalTime EVENING_END = LocalTime.of(22, 0);

    private static final int PREFERENCE_SCORE_PREFERRED = 1;
    private static final int PREFERENCE_SCORE_NEUTRAL = 0;
    private static final int PREFERENCE_SCORE_UNPREFERRED = -1; // Represents a hard constraint violation in fitness

    // Define KEYS for configuration settings
    private static final String GA_POPULATION_SIZE = "ga.population.size";
    private static final String GA_MAX_GENERATIONS = "ga.max.generations";
    private static final String GA_MUTATION_RATE = "ga.mutation.rate";
    private static final String GA_CROSSOVER_RATE = "ga.crossover.rate";
    private static final String GA_TOURNAMENT_SIZE = "ga.tournament.size";
    private static final String PENALTY_HARD_CONSTRAINT_KEY = "penalty.hard.constraint";
    private static final String PENALTY_UNDER_STAFFING_KEY = "penalty.under.staffing"; // Base weight
    private static final String PENALTY_OVER_STAFFING_KEY = "penalty.over.staffing";   // Base weight
    private static final String PENALTY_FORBIDDEN_SUCCESSION_KEY = "penalty.forbidden.succession";
    private static final String LATE_SHIFT_THRESHOLD_KEY = "threshold.late.shift.end";
    private static final String EARLY_SHIFT_THRESHOLD_KEY = "threshold.early.shift.start";

    // --- GA Configuration (Defaults, loaded from config service) ---
    /** Number of candidate schedules (chromosomes) in each generation. Larger populations explore more solutions but take longer. */
    private int populationSize = 50;
    /** Maximum number of generations the GA will run. More generations allow for better optimization but increase runtime. */
    private int maxGenerations = 100;
    /** Probability (0.0 to 1.0) o f a gene (assignment) being randomly changed during mutation. Helps introduce diversity and escape local optima. */
    private double mutationRate = 0.1;
    /** Probability (0.0 to 1.0) that selected parents will exchange genetic material (parts of their schedules) to create offspring. */
    private double crossoverRate = 0.8;
    /** Number of chromosomes randomly selected to compete in tournament selection; the fittest wins and becomes a parent. Larger size increases selection pressure. */
    private int tournamentSize = 5;

    // --- Penalty Weights (Defaults, loaded from config service) ---
    /** Penalty applied for violating hard constraints (e.g., skill mismatch, unavailability). Should be significantly higher than soft constraint penalties. */
    private int penaltyHardConstraint = 500;
    /** Base penalty applied per missing employee below the task's minimum coverage, modulated by task's penaltyWeight. */
    private int penaltyUnderStaffingBase = 350;
    /** Base penalty applied per extra employee above the task's optimal coverage, modulated by task's penaltyWeight. Usually lower than under-staffing penalty. */
    private int penaltyOverStaffingBase = 30;
    /** Penalty for assigning an employee to an early shift immediately following a late shift on the previous day. */
    private int penaltyForbiddenSuccession = 200;

    // --- Thresholds (Defaults, loaded from config service) ---
    /** Time after which a shift ending is considered 'late' for forbidden succession checks. */
    private LocalTime lateShiftThreshold = LocalTime.of(19, 59);
    /** Time before which a shift starting is considered 'early' for forbidden succession checks. */
    private LocalTime earlyShiftThreshold = LocalTime.of(8, 0);

    private static final Random random = new Random();

    // Method to load configuration on service initialization or before generation
    // @PostConstruct // Option: Load once on startup
    private void loadConfiguration() {
        log.info("Loading configuration settings for ScheduleService...");
        populationSize = Integer.parseInt(configurationService.getSettingOrDefault(GA_POPULATION_SIZE, "50"));
        maxGenerations = Integer.parseInt(configurationService.getSettingOrDefault(GA_MAX_GENERATIONS, "100"));
        mutationRate = Double.parseDouble(configurationService.getSettingOrDefault(GA_MUTATION_RATE, "0.1"));
        crossoverRate = Double.parseDouble(configurationService.getSettingOrDefault(GA_CROSSOVER_RATE, "0.8"));
        tournamentSize = Integer.parseInt(configurationService.getSettingOrDefault(GA_TOURNAMENT_SIZE, "5"));

        penaltyHardConstraint = Integer.parseInt(configurationService.getSettingOrDefault(PENALTY_HARD_CONSTRAINT_KEY, "1000"));
        penaltyUnderStaffingBase = Integer.parseInt(configurationService.getSettingOrDefault(PENALTY_UNDER_STAFFING_KEY, "500"));
        penaltyOverStaffingBase = Integer.parseInt(configurationService.getSettingOrDefault(PENALTY_OVER_STAFFING_KEY, "50"));
        penaltyForbiddenSuccession = Integer.parseInt(configurationService.getSettingOrDefault(PENALTY_FORBIDDEN_SUCCESSION_KEY, "200"));

        try {
            lateShiftThreshold = LocalTime.parse(configurationService.getSettingOrDefault(LATE_SHIFT_THRESHOLD_KEY, "19:59"));
            earlyShiftThreshold = LocalTime.parse(configurationService.getSettingOrDefault(EARLY_SHIFT_THRESHOLD_KEY, "08:00"));
        } catch (DateTimeParseException e) {
            log.error("Error parsing time threshold settings, using defaults.", e);
            lateShiftThreshold = LocalTime.of(19, 59);
            earlyShiftThreshold = LocalTime.of(8, 0);
        }
        log.info("Configuration loaded: PopSize={}, MaxGen={}, MutRate={}, CrossRate={}, TournSize={}, PenaltyHard={}, etc.",
                populationSize, maxGenerations, mutationRate, crossoverRate, tournamentSize, penaltyHardConstraint);
    }

    /**
     * Deletes all shifts within the specified date range.
     *
     * @param startDate The start date of the range (inclusive).
     * @param endDate   The end date of the range (inclusive).
     */
    public void deleteSchedule(LocalDate startDate, LocalDate endDate) {
        log.info("Deleting schedule between {} and {}", startDate, endDate);
        shiftRepository.deleteByDateRange(startDate, endDate);
        log.info("Deleted shifts between {} and {}", startDate, endDate);
    }

    // Define the structure for required shifts
    // TODO: Replace with a more flexible way to define coverage requirements (min/opt per skill/task/time) from DB/config
    private record ShiftDefinition(LocalTime startTime, LocalTime endTime, String taskName) {}
    private static final List<ShiftDefinition> DAILY_SHIFT_DEFINITIONS = List.of(
        new ShiftDefinition(LocalTime.of(9, 0), LocalTime.of(13, 0), "Morning Task"),
        new ShiftDefinition(LocalTime.of(13, 0), LocalTime.of(17, 0), "Afternoon Task"),
        new ShiftDefinition(LocalTime.of(17, 0), LocalTime.of(21, 0), "Evening Task")
    );

    /**
     * Retrieves the existing schedule for the specified date range.
     * Eagerly loads employee skills and task skills to prevent lazy loading issues.
     *
     * @param startDate The start date of the range (inclusive).
     * @param endDate   The end date of the range (inclusive).
     * @return A list of ShiftDTO objects representing the schedule.
     */
    @Transactional(readOnly = true)
    public List<ShiftDTO> getSchedule(LocalDate startDate, LocalDate endDate) {
        // Fetch shifts for the period
        List<Shift> shifts = shiftRepository.findByShiftDateBetweenOrderByShiftDateAscStartTimeAsc(startDate, endDate);
        // Eagerly load related entities to prevent lazy loading issues during DTO mapping outside a transaction
        shifts.forEach(shift -> {
            if (shift.getEmployee() != null) Hibernate.initialize(shift.getEmployee().getSkills());
            if (shift.getTask() != null) Hibernate.initialize(shift.getTask().getRequiredSkills());
        });
        return shifts.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    /**
     * Generates a new schedule for the specified date range using a Genetic Algorithm.
     * 1. Clears any existing shifts in the range.
     * 2. Fetches active employees and tasks.
     * 3. Generates required shift slots based on hardcoded definitions (TODO: make dynamic).
     * 4. Runs the GA to find an optimal schedule chromosome.
     * 5. Converts the best chromosome into persistable Shift entities.
     * 6. Saves the new shifts to the database.
     * 7. Returns the generated schedule as a list of DTOs.
     *
     * @param request DTO containing the start and end dates for the schedule generation.
     * @return A list of ShiftDTO objects representing the newly generated schedule.
     * @throws IllegalArgumentException if the date range in the request is invalid.
     */
    @Transactional
    public ScheduleGenerationResultDTO generateSchedule(ScheduleRequestDTO request) {
        loadConfiguration(); // Load latest config before generating
        log.info("BEGIN generateSchedule for period: {} to {}", request.getStartDate(), request.getEndDate());
        List<String> explanations = new ArrayList<>();

        // --- Input Validation ---
        if (request.getStartDate() == null || request.getEndDate() == null || request.getEndDate().isBefore(request.getStartDate())) {
            log.error("generateSchedule failed: Invalid date range provided.");
            explanations.add("Invalid date range provided.");
            return new ScheduleGenerationResultDTO(List.of(), explanations);
        }

        // 1. Clear existing schedule for the period (ensure clean slate)
        log.info("Clearing existing shifts from {} to {}", request.getStartDate(), request.getEndDate());
        shiftRepository.deleteByDateRange(request.getStartDate(), request.getEndDate());
        log.info("Finished clearing existing shifts.");

        // 2. Get necessary data (Employees, Tasks)
        log.info("Fetching active employees...");
        List<Employee> employees = employeeRepository.findAllByIsActive(true);
        log.info("Found {} active employees.", employees.size());
        if (employees.isEmpty()) {
            log.warn("No active employees found. Cannot generate schedule.");
            explanations.add("No active employees found. Please ensure there are active employees in the system.");
            log.info("END generateSchedule: Returning empty list due to no active employees.");
            return new ScheduleGenerationResultDTO(List.of(), explanations);
        }

        log.info("Fetching all tasks...");
        Map<String, Task> taskMap = taskRepository.findAll().stream()
                                        .collect(Collectors.toMap(Task::getName, t -> t, (t1, t2) -> t1)); // Handle potential duplicate task names
        log.info("Found {} unique tasks.", taskMap.size());


        // Eagerly load employee skills ONCE (critical for performance in GA fitness checks)
        log.debug("Eagerly loading skills for {} employees...", employees.size());
        employees.forEach(e -> Hibernate.initialize(e.getSkills()));
        // Eagerly load task required skills ONCE
        taskMap.values().forEach(t -> Hibernate.initialize(t.getRequiredSkills()));
        log.debug("Skill loading complete.");


        // --- Define Required Shift Slots for the entire period ---
        log.info("Generating required shift slots based on definitions...");
        // This represents the "demand" that the GA needs to fulfill.
        // Currently based on the simple DAILY_SHIFT_DEFINITIONS. This should be more dynamic.
        // TODO: Load coverage requirements (min/opt per task/skill/time) from DB/config instead of hardcoded definitions
        List<RequiredShiftSlot> requiredSlots = generateRequiredSlots(request.getStartDate(), request.getEndDate(), taskMap);
        log.info("Generated {} required shift slots to be filled between {} and {}.",
                 requiredSlots.size(), request.getStartDate(), request.getEndDate());
        if (requiredSlots.isEmpty()) {
            log.warn("No required shift slots defined for the period (based on current definitions). Cannot generate schedule.");
            explanations.add("No required shift slots defined for the period based on current shift/task definitions. Please check shift/task configuration.");
            log.info("END generateSchedule: Returning empty list due to no required shift slots.");
            return new ScheduleGenerationResultDTO(List.of(), explanations);
        }

        // 3. Setup and Run the Genetic Algorithm
        log.info("Initializing Genetic Algorithm Engine...");
        GeneticAlgorithmEngine gaEngine = new GeneticAlgorithmEngine(
                employees, taskMap, requiredSlots, request.getStartDate(), request.getEndDate()
        );

        // --- Run the GA --- (Pass loaded config)
        log.info("Starting Genetic Algorithm execution (Pop: {}, MaxGen: {})...", populationSize, maxGenerations);
        ScheduleChromosome bestSchedule = gaEngine.run(populationSize, maxGenerations, crossoverRate, mutationRate, tournamentSize);
        log.info("Genetic Algorithm finished. Best schedule fitness found: {}", bestSchedule.getFitness());
        if (bestSchedule.getFitness() > 0) {
             log.warn("The best schedule found still has constraint violations (Fitness > 0). Review penalties and constraints.");
        }

        // 4. Convert the best chromosome (List<ShiftAssignment>) to persistable Shift entities
        log.info("Converting best chromosome to Shift entities...");
        List<Shift> finalShifts = convertChromosomeToShifts(bestSchedule, employees);
        log.info("Converted {} assignments with non-null employees into Shift objects.", finalShifts.size());


        // 5. Save the generated shifts
        if (finalShifts.isEmpty()) {
            log.warn("No shifts to save from the GA result.");
            // Analyze why no shifts could be assigned
            explanations.add("No shifts could be assigned. This may be due to overly restrictive constraints such as employee availability, contract hours, skill requirements, or penalty weights. Review constraint settings and employee/task data.");
        } else {
            log.info("Saving {} generated shifts...", finalShifts.size());
            List<Shift> savedShifts = shiftRepository.saveAll(finalShifts);
            log.info("Successfully saved {} new shifts from the best GA solution.", savedShifts.size());

            // 6. Return DTOs for the generated schedule
            log.info("Mapping {} saved shifts to DTOs.", savedShifts.size());
            List<ShiftDTO> resultDTOs = savedShifts.stream().map(this::mapToDTO).collect(Collectors.toList());
            log.info("END generateSchedule: Returning {} ShiftDTOs.", resultDTOs.size());
            return new ScheduleGenerationResultDTO(resultDTOs, explanations);
        }

        // If finalShifts was empty, return explanations
        log.info("END generateSchedule: Returning empty list as no shifts were generated or saved.");
        return new ScheduleGenerationResultDTO(List.of(), explanations);
    }

     // Helper to generate the list of required shifts based on current simple definitions
     // TODO: Replace this with logic to read actual coverage requirements
    private List<RequiredShiftSlot> generateRequiredSlots(LocalDate startDate, LocalDate endDate, Map<String, Task> taskMap) {
        List<RequiredShiftSlot> slots = new ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            for (ShiftDefinition def : DAILY_SHIFT_DEFINITIONS) {
                Task task = taskMap.get(def.taskName());
                // Only add slot if task exists (assuming task is mandatory for the defined shift)
                // TODO: Make task requirement configurable per coverage need
                if (task != null) {
                     // Skills should already be initialized from step 2
                     slots.add(new RequiredShiftSlot(currentDate, def.startTime(), def.endTime(), task));
                } else {
                    log.warn("Task '{}' defined in ShiftDefinition not found in database for {}. Skipping required slot generation for this definition.", def.taskName(), currentDate);
                }
            }
            currentDate = currentDate.plusDays(1);
        }
        return slots;
    }

    // Helper to convert the GA chromosome result (best solution) into persistable Shift entities
    private List<Shift> convertChromosomeToShifts(ScheduleChromosome chromosome, List<Employee> employees) {
         List<Shift> shifts = new ArrayList<>();
         // Create a map for quick lookup of employee entities by ID
         Map<Long, Employee> employeeMap = employees.stream().collect(Collectors.toMap(Employee::getId, e -> e));

         for (ShiftAssignment assignment : chromosome.getAssignments()) {
             // Only create Shift entities for slots that have an employee assigned in the chromosome
             if (assignment.employeeId() != null) {
                 Employee assignedEmployee = employeeMap.get(assignment.employeeId());
                 if (assignedEmployee == null) {
                     log.error("Consistency Error: Employee ID {} found in chromosome but not in employee map!", assignment.employeeId());
                     continue; // Skip this assignment if employee data is inconsistent
                 }
                 Shift shift = new Shift();
                 shift.setEmployee(assignedEmployee);
                 // Task comes directly from the RequiredShiftSlot definition
                 shift.setTask(assignment.requiredSlot().task());
                 shift.setShiftDate(assignment.requiredSlot().date());
                 shift.setStartTime(assignment.requiredSlot().startTime());
                 shift.setEndTime(assignment.requiredSlot().endTime());
                 shifts.add(shift);
             }
             // Note: We are currently NOT creating 'Unassigned' shift records for unfilled slots.
             // This could be added if the application requires visibility of unfilled demand.
         }
         return shifts;
    }

    // ========================================================================
    // === Genetic Algorithm Components (Defined as Inner Classes) ==========
    // ========================================================================

    /**
     * Represents a single required shift slot that needs an employee assigned.
     * Includes the date, time, and the task (which implies skill requirements).
     */
    private record RequiredShiftSlot(LocalDate date, LocalTime startTime, LocalTime endTime, Task task) {}

    /**
     * Represents an assignment within a schedule chromosome. Links a RequiredShiftSlot
     * to an Employee ID. employeeId can be null if the slot is unassigned in this chromosome.
     */
    private record ShiftAssignment(RequiredShiftSlot requiredSlot, Long employeeId) {}

    /**
     * Represents a complete candidate schedule (a chromosome in the GA).
     * It's essentially a list of assignments, one for each RequiredShiftSlot.
     * Fitness score represents the total penalty; lower is better (0 is optimal).
     */
    private static class ScheduleChromosome {
        private final List<ShiftAssignment> assignments; // The "genes" of the chromosome
        @Setter
        @Getter
        private double fitness = -1.0; // Cached fitness score (negative means not calculated yet). Lower is better.

        // Constructor creates a defensive copy of the assignments list
        public ScheduleChromosome(List<ShiftAssignment> assignments) {
            this.assignments = new ArrayList<>(assignments);
        }

        // Provides read-only access to the assignments
        public List<ShiftAssignment> getAssignments() {
            return Collections.unmodifiableList(assignments);
        }

        // Allows modification of a specific assignment (used by mutation/crossover)
        // Invalidate fitness cache upon modification.
        public void setAssignment(int index, Long employeeId) {
             if (index >= 0 && index < assignments.size()) {
                ShiftAssignment oldAssignment = assignments.get(index);
                // Create a new ShiftAssignment record with the updated employeeId
                assignments.set(index, new ShiftAssignment(oldAssignment.requiredSlot(), employeeId));
                this.fitness = -1.0; // Fitness needs recalculation
             } else {
                 log.warn("Attempted to set assignment at invalid index: {}", index);
             }
        }

        public int size() {
            return assignments.size();
        }

        // Creates a deep copy (new list and new assignments pointing to same slots/ids)
        public ScheduleChromosome copy() {
             return new ScheduleChromosome(new ArrayList<>(this.assignments));
        }

        @Override
        public String toString() {
            // Provides a basic string representation for logging purposes
            long assignedCount = assignments.stream().filter(a -> a.employeeId() != null).count();
            return String.format("Chromosome{size=%d, assigned=%d, fitness=%.2f}",
                                 assignments.size(), assignedCount, fitness);
        }
    }

    /**
     * Calculates the fitness of a given schedule chromosome based on constraint violations.
     * Fitness is calculated as the sum of penalties for hard and soft constraint violations.
     * A lower fitness score indicates a better schedule (0 is optimal).
     * Loads penalty weights and thresholds from the outer ScheduleService instance.
     */
    private class FitnessCalculator {
        private final List<Employee> employees;
        private final Map<Long, Employee> employeeMap;
        private final List<RequiredShiftSlot> requiredSlots;
        private final Map<LocalDate, Boolean> isWeekendMap; // Precomputed weekend days

        // Load constants from ScheduleService outer class instance
        private final int currentPenaltyHardConstraint = ScheduleService.this.penaltyHardConstraint;
        private final int currentPenaltyUnderStaffingBase = ScheduleService.this.penaltyUnderStaffingBase;
        private final int currentPenaltyOverStaffingBase = ScheduleService.this.penaltyOverStaffingBase;
        private final int currentPenaltyForbiddenSuccession = ScheduleService.this.penaltyForbiddenSuccession;
        private final LocalTime currentLateShiftThreshold = ScheduleService.this.lateShiftThreshold;
        private final LocalTime currentEarlyShiftThreshold = ScheduleService.this.earlyShiftThreshold;

        public FitnessCalculator(List<Employee> employees, List<RequiredShiftSlot> requiredSlots, LocalDate startDate, LocalDate endDate) {
            this.employees = employees;
            this.employeeMap = employees.stream().collect(Collectors.toMap(Employee::getId, e -> e));
            this.requiredSlots = requiredSlots;
            this.isWeekendMap = precomputeWeekends(startDate, endDate);
        }

        // Helper to precompute which dates are weekends for faster lookup
        private Map<LocalDate, Boolean> precomputeWeekends(LocalDate start, LocalDate end) {
            Map<LocalDate, Boolean> map = new HashMap<>();
            LocalDate current = start;
            while (!current.isAfter(end)) {
                DayOfWeek day = current.getDayOfWeek();
                map.put(current, day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);
                current = current.plusDays(1);
            }
            return map;
        }

        /**
         * Calculates the total penalty score (fitness) for a given chromosome.
         * Iterates through required slots (for coverage checks) and employee assignments
         * (for individual constraints and preferences), summing up penalties.
         *
         * @param chromosome The schedule chromosome to evaluate.
         * @return The total penalty score (fitness). Lower is better.
         */
        public double calculateFitness(ScheduleChromosome chromosome) {
            double totalPenalty = 0;

            // --- Pre-process: Group assignments for efficient checks ---
            Map<Long, List<ShiftAssignment>> assignmentsByEmployee = new HashMap<>();
            Map<RequiredShiftSlot, Long> assignmentMap = new HashMap<>();
            Map<LocalDate, List<ShiftAssignment>> assignmentsByDate = new HashMap<>();
            // Track assigned counts per task/date/time (approximated by slot for now)
            Map<RequiredShiftSlot, Integer> assignedCountPerSlot = new HashMap<>();
            requiredSlots.forEach(slot -> assignedCountPerSlot.put(slot, 0)); // Initialize counts

            for (ShiftAssignment assignment : chromosome.getAssignments()) {
                 assignmentMap.put(assignment.requiredSlot(), assignment.employeeId());
                 assignmentsByDate.computeIfAbsent(assignment.requiredSlot().date(), k -> new ArrayList<>()).add(assignment);

                 if (assignment.employeeId() != null) {
                     assignmentsByEmployee.computeIfAbsent(assignment.employeeId(), k -> new ArrayList<>()).add(assignment);
                     // Increment assigned count for the specific slot
                     assignedCountPerSlot.compute(assignment.requiredSlot(), (slot, count) -> (count == null) ? 1 : count + 1);
                 } // Unassigned slots handled later by coverage check
            }

            // --- PENALTY: S1 - Under/Over Staffing (based on Task min/opt coverage) ---
            // Iterate through the required slots (demand)
            for (RequiredShiftSlot slot : requiredSlots) {
                Task task = slot.task();
                int assignedCount = assignedCountPerSlot.getOrDefault(slot, 0);
                int minCoverage = task.getMinimumCoverage() != null ? task.getMinimumCoverage() : 1;
                int optCoverage = task.getOptimalCoverage() != null ? task.getOptimalCoverage() : minCoverage;
                int taskWeight = task.getPenaltyWeight() != null ? task.getPenaltyWeight() : 10;

                if (assignedCount < minCoverage) {
                    // Use loaded base penalty * task weight
                    totalPenalty += (minCoverage - assignedCount) * taskWeight * (currentPenaltyUnderStaffingBase / 10.0); // Adjust base as needed
                    log.trace("Fitness Penalty (Understaff): Slot {} needs min {}, has {}. Penalty: {}", slot, minCoverage, assignedCount, (minCoverage - assignedCount) * taskWeight * (currentPenaltyUnderStaffingBase / 10.0));
                } else if (assignedCount > optCoverage) {
                    // Use loaded base penalty * task weight
                    totalPenalty += (assignedCount - optCoverage) * taskWeight * (currentPenaltyOverStaffingBase / 10.0);
                    log.trace("Fitness Penalty (Overstaff): Slot {} needs opt {}, has {}. Penalty: {}", slot, optCoverage, assignedCount, (assignedCount - optCoverage) * taskWeight * (currentPenaltyOverStaffingBase / 10.0));
                }
                // No penalty if assignedCount >= minCoverage && assignedCount <= optCoverage
            }


            // --- Check Constraints per Employee ---
            for (Map.Entry<Long, List<ShiftAssignment>> entry : assignmentsByEmployee.entrySet()) {
                Long employeeId = entry.getKey();
                Employee employee = employeeMap.get(employeeId);
                if (employee == null) continue;

                List<ShiftAssignment> employeeAssignments = entry.getValue();
                employeeAssignments.sort(Comparator.comparing((ShiftAssignment a) -> a.requiredSlot().date())
                                                 .thenComparing(a -> a.requiredSlot().startTime()));

                Map<LocalDate, Integer> shiftsPerDayCount = new HashMap<>();
                ShiftAssignment previousAssignment = null;
                int consecutiveWorkDays = 0;
                int consecutiveFreeDays = 0; // Need to track this too
                LocalDate lastWorkDay = null;
                Duration totalDuration = Duration.ZERO;
                int weekendsWorked = 0;
                int consecutiveWeekendsWorked = 0; // Harder to track accurately without history
                boolean workedThisWeekend = false;
                LocalDate weekendStartDate = null;

                for (int i = 0; i < employeeAssignments.size(); i++) {
                    ShiftAssignment currentAssignment = employeeAssignments.get(i);
                    RequiredShiftSlot currentSlot = currentAssignment.requiredSlot();
                    LocalDate currentDate = currentSlot.date();
                    LocalTime currentStart = currentSlot.startTime();
                    LocalTime currentEnd = currentSlot.endTime();
                    Task currentTask = currentSlot.task();

                    if (currentTask == null) {
                         log.error("Fitness Error: Task is null for required slot {} assigned to {}", currentSlot, employee.getName());
                         totalPenalty += currentPenaltyHardConstraint * 10;
                         continue;
                     }

                    // --- PENALTY: H1 - Single Assignment Per Day ---
                    int dayShiftCount = shiftsPerDayCount.getOrDefault(currentDate, 0) + 1;
                    shiftsPerDayCount.put(currentDate, dayShiftCount);
                    if (dayShiftCount > 1) {
                        totalPenalty += currentPenaltyHardConstraint;
                        log.trace("Fitness Penalty (H1): {} has {} shifts on {}", employee.getName(), dayShiftCount, currentDate);
                    }

                    // --- PENALTY: H4 - Missing Required Skill ---
                    Set<Skill> requiredSkills = currentTask.getRequiredSkills();
                    if (requiredSkills != null && !requiredSkills.isEmpty()) {
                        if (!employee.getSkills().containsAll(requiredSkills)) {
                            totalPenalty += currentPenaltyHardConstraint;
                            log.trace("Fitness Penalty (H4): {} missing skills ({}) for task '{}' ({}) on {}",
                                      employee.getName(), employee.getSkills().stream().map(Skill::getName).collect(Collectors.joining(",")),
                                      currentTask.getName(), requiredSkills.stream().map(Skill::getName).collect(Collectors.joining(",")),
                                      currentDate);
                        }
                    }

                    // --- PENALTY: Employee Availability Check ---
                    if (!isEmployeeAvailable(employee, currentDate, currentStart, currentEnd)) {
                         totalPenalty += currentPenaltyHardConstraint;
                         log.trace("Fitness Penalty (Avail): {} not available for shift {}:{} on {}",
                                   employee.getName(), currentStart, currentEnd, currentDate);
                    }

                    // --- PENALTY: H3 - Forbidden Successions (Late -> Early) ---
                     if (previousAssignment != null) {
                         RequiredShiftSlot prevSlot = previousAssignment.requiredSlot();
                         if (prevSlot.date().equals(currentDate.minusDays(1))) {
                             boolean prevWasLate = prevSlot.endTime().isAfter(currentLateShiftThreshold);
                             boolean currentIsEarly = currentStart.isBefore(currentEarlyShiftThreshold);
                             if (prevWasLate && currentIsEarly) {
                                 totalPenalty += currentPenaltyForbiddenSuccession;
                                 log.trace("Fitness Penalty (H3): {} has forbidden succession: {} {} -> {} {}. Penalty: {}",
                                           employee.getName(), prevSlot.date(), prevSlot.endTime(), currentDate, currentStart, currentPenaltyForbiddenSuccession);
                             }
                         }
                     }

                    // --- SOFT CONSTRAINT CHECKS using Employee settings ---

                    // Update Total Duration
                    totalDuration = totalDuration.plus(Duration.between(currentStart, currentEnd));

                    // Update Consecutive Days Tracking
                    if (lastWorkDay != null) {
                        long daysBetween = ChronoUnit.DAYS.between(lastWorkDay, currentDate);
                        if (daysBetween == 1) {
                            consecutiveWorkDays++;
                            consecutiveFreeDays = 0;
                        } else if (daysBetween > 1) {
                            // Check min consecutive free days violation here if needed
                            consecutiveFreeDays = (int) daysBetween - 1;
                            consecutiveWorkDays = 1; // Reset workdays count
                        }
                        // If daysBetween == 0, it's the same day (handled by H1 check)
                    } else {
                        consecutiveWorkDays = 1; // First assignment in the list
                    }
                    lastWorkDay = currentDate;

                     // --- PENALTY: S2 - Max Consecutive Working Days ---
                    int maxConsecutive = employee.getMaxConsecutiveDays() != null ? employee.getMaxConsecutiveDays() : 999;
                    if (consecutiveWorkDays > maxConsecutive) {
                        int weight = employee.getConsecutiveDayPenaltyWeight() != null ? employee.getConsecutiveDayPenaltyWeight() : 5;
                        totalPenalty += (consecutiveWorkDays - maxConsecutive) * weight;
                        log.trace("Fitness Penalty (MaxConsDays): {} worked {} > max {}. Penalty: {}", employee.getName(), consecutiveWorkDays, maxConsecutive, (consecutiveWorkDays - maxConsecutive) * weight);
                    }

                    // Check Min Consecutive Working Days only when a break occurs (or at the end)
                    // We'll do a final check after the loop for min consecutive days/free days.


                    // --- Weekend Tracking & Penalties --- S4 / S6 Part --- 
                    boolean isCurrentWeekend = isWeekendMap.getOrDefault(currentDate, false);
                    if (isCurrentWeekend) {
                        if (!workedThisWeekend) {
                            // Start of a weekend working block
                            workedThisWeekend = true;
                            weekendsWorked++;
                            weekendStartDate = currentDate;
                            // TODO: Implement consecutive weekend check (requires history or more complex tracking)
                            // int maxConsecWeekends = employee.getMaxConsecutiveWeekends(); ...
                        }
                    } else { // It's a weekday
                        workedThisWeekend = false; // Reset weekend flag if it's not Saturday/Sunday
                        weekendStartDate = null;
                    }


                    // --- PENALTY: S5 - Preferences (Unpreferred/Preferred) ---
                    int prefScore = getShiftPreferenceScore(employee, currentDate, currentStart, currentEnd);
                    if (prefScore == PREFERENCE_SCORE_UNPREFERRED) {
                        int weight = employee.getTotalHoursPenaltyWeight() != null ? employee.getTotalHoursPenaltyWeight() : 2;
                        totalPenalty += weight * 10.0; // High penalty for unpreferred
                        log.trace("Fitness Penalty (Pref): {} Unpreferred shift {}:{} on {}. Penalty: {}",
                                  employee.getName(), currentStart, currentEnd, currentDate, weight * 10.0);
                    } else if (prefScore == PREFERENCE_SCORE_NEUTRAL) {
                        totalPenalty += 1; // Small penalty to encourage preferred
                    }

                    previousAssignment = currentAssignment;

                } // End loop through employee's assignments for this chromosome

                // --- Final Checks for the Employee (after iterating all their shifts) ---
                // We need to analyze the sequence of work/free days based on the sorted assignments

                // Add penalty if the last block of work didn't meet min consecutive days
                if (consecutiveWorkDays > 0) {
                    int minConsecutive = employee.getMinConsecutiveDays() != null ? employee.getMinConsecutiveDays() : 0;
                     if (minConsecutive > 0 && consecutiveWorkDays < minConsecutive) {
                         int weight = employee.getConsecutiveDayPenaltyWeight() != null ? employee.getConsecutiveDayPenaltyWeight() : 5;
                         totalPenalty += (minConsecutive - consecutiveWorkDays) * weight * 2.0; // Heavier penalty for min violation
                         log.trace("Fitness Penalty (MinConsDays): {} ended work block with {} < min {}. Penalty: {}", employee.getName(), consecutiveWorkDays, minConsecutive, (minConsecutive - consecutiveWorkDays) * weight * 2.0);
                     }
                }
                // TODO: Implement min consecutive FREE days check. Requires tracking free day blocks explicitly.

                // --- PENALTY: S6 - Min/Max Total Hours ---
                long totalMinutes = totalDuration.toMinutes();
                int minTotal = (employee.getMinTotalHours() != null ? employee.getMinTotalHours() : 0) * 60;
                int maxTotal = (employee.getMaxTotalHours() != null ? employee.getMaxTotalHours() : Integer.MAX_VALUE / 60) * 60;
                // Use contract hours as a default max if maxTotalHours is null? Decision needed.
                // int contractMinutes = employee.getContractHours() * 60;
                // maxTotal = Math.min(maxTotal, contractMinutes > 0 ? contractMinutes * 1.2 : Integer.MAX_VALUE); // Example: Allow 20% over contract if no specific max

                int weightHours = employee.getTotalHoursPenaltyWeight() != null ? employee.getTotalHoursPenaltyWeight() : 2;
                if (totalMinutes < minTotal) {
                    totalPenalty += (minTotal - totalMinutes) * weightHours * 0.1; // Penalty proportional to deficit
                    log.trace("Fitness Penalty (MinHours): {} worked {}m < min {}m. Penalty: {}", employee.getName(), totalMinutes, minTotal, (minTotal - totalMinutes) * weightHours * 0.1);
                }
                if (totalMinutes > maxTotal) {
                    totalPenalty += (totalMinutes - maxTotal) * weightHours * 0.1; // Penalty proportional to excess
                     log.trace("Fitness Penalty (MaxHours): {} worked {}m > max {}m. Penalty: {}", employee.getName(), totalMinutes, maxTotal, (totalMinutes - maxTotal) * weightHours * 0.1);
                }

                // --- PENALTY: S6 - Max Weekends Worked ---
                int maxWW = employee.getMaxWeekends() != null ? employee.getMaxWeekends() : 99;
                if (weekendsWorked > maxWW) {
                    int weightWeekend = employee.getWeekendPenaltyWeight() != null ? employee.getWeekendPenaltyWeight() : 10;
                    totalPenalty += (weekendsWorked - maxWW) * weightWeekend;
                    log.trace("Fitness Penalty (MaxWeekends): {} worked {} > max {}. Penalty: {}", employee.getName(), weekendsWorked, maxWW, (weekendsWorked - maxWW) * weightWeekend);
                }

                // --- PENALTY: S2 - Min Consecutive Days (if applicable) ---
                // This requires more state tracking within the loop or a post-processing step
                // to identify all work blocks and check their lengths against minConsecutiveDays.
                // Skipping for now for brevity, but should be added for full S2 compliance.

                // --- PENALTY: S3 - Min Consecutive Free Days --- 
                // Similar to Min Consecutive Work Days, needs better state tracking or post-processing.
                // Skipping for now.

            } // End loop through employees

            return totalPenalty;
        }

    } // End FitnessCalculator


    /**
     * Manages the execution of the Genetic Algorithm to generate schedules.
     * Handles population initialization, selection, crossover, mutation, and evaluation
     * over multiple generations to find a near-optimal schedule.
     */
    // Main GA Engine
    private class GeneticAlgorithmEngine {
        private final List<Employee> employees;
        private final Map<String, Task> taskMap; // For context if needed
        private final List<RequiredShiftSlot> requiredSlots; // The "problem" definition
        private final LocalDate startDate; // For context if needed
        private final LocalDate endDate;   // For context if needed
        private final FitnessCalculator fitnessCalculator;
        private List<ScheduleChromosome> population;

        public GeneticAlgorithmEngine(List<Employee> employees, Map<String, Task> taskMap,
                                    List<RequiredShiftSlot> requiredSlots, LocalDate startDate, LocalDate endDate) {
            this.employees = employees;
            this.taskMap = taskMap;
            this.requiredSlots = requiredSlots;
            this.startDate = startDate;
            this.endDate = endDate;
            // Pass required slots and date range to FitnessCalculator constructor
            this.fitnessCalculator = new FitnessCalculator(employees, requiredSlots, startDate, endDate);
            this.population = new ArrayList<>(populationSize);
        }

        /**
         * Runs the genetic algorithm for a configured number of generations.
         * Initializes population, then iteratively applies selection, crossover,
         * and mutation to evolve the population towards better fitness scores.
         * Uses elitism to preserve the best individual from each generation.
         *
         * @param popSize Popuplation size.
         * @param maxGen Maximum number of generations.
         * @param crossRate Crossover rate.
         * @param mutRate Mutation rate.
         * @param tourneySize Tournament size for selection.
         * @return The best ScheduleChromosome (lowest fitness) found after all generations.
         */
        public ScheduleChromosome run(int popSize, int maxGen, double crossRate, double mutRate, int tourneySize) {
            log.info("GA Run: Initializing population (Size: {})...", popSize);
            initializePopulation(popSize);
            log.info("GA Run: Evaluating initial population...");
            evaluatePopulation(population); // Initial fitness evaluation
             log.info("GA Run: Initial population evaluation complete. Best initial fitness: {}", population.isEmpty() ? "N/A" : population.get(0).getFitness());


            for (int generation = 1; generation <= maxGen; generation++) {
                 log.debug("GA Generation {} starting...", generation);
                List<ScheduleChromosome> newPopulation = new ArrayList<>(popSize);

                // Elitism: Preserve the best individual from the current population
                 Collections.sort(population, Comparator.comparingDouble(ScheduleChromosome::getFitness));
                 if (!population.isEmpty()) {
                    log.trace("GA Gen {}: Preserving elite chromosome (Fitness: {})", generation, population.get(0).getFitness());
                     newPopulation.add(population.get(0).copy()); // Add a copy of the best
                 }

                // Generate the rest of the new population through selection, crossover, mutation
                while (newPopulation.size() < popSize) {
                    // Select two parents based on fitness
                    ScheduleChromosome parent1 = tournamentSelection(tourneySize);
                    ScheduleChromosome parent2 = tournamentSelection(tourneySize);
                     log.trace("GA Gen {}: Selected parents (Fitness: {}, {})", generation, parent1.getFitness(), parent2.getFitness());
                    ScheduleChromosome offspring;

                    // Apply Crossover
                    if (random.nextDouble() < crossRate) {
                        offspring = crossover(parent1, parent2);
                         log.trace("GA Gen {}: Crossover applied.", generation);
                    } else {
                        // If no crossover, clone one parent (e.g., the fitter one)
                        offspring = parent1.getFitness() <= parent2.getFitness() ? parent1.copy() : parent2.copy();
                        log.trace("GA Gen {}: Crossover skipped, cloned parent.", generation);
                    }

                    // Apply Mutation
                    mutate(offspring, mutRate); // Mutate might log internally if needed

                    // Add the new offspring to the next generation's population
                    newPopulation.add(offspring);
                }

                population = newPopulation; // Replace old population with the new one
                 log.trace("GA Gen {}: Evaluating new population ({} individuals)...", generation, population.size());
                evaluatePopulation(population); // Evaluate fitness of the newly generated population
                 Collections.sort(population, Comparator.comparingDouble(ScheduleChromosome::getFitness)); // Sort for logging the best

                 // Log progress - adjust frequency if too verbose (e.g., log every 10 generations)
                 if (generation % 10 == 0 || generation == maxGen || generation == 1) {
                    log.debug("GA Generation {}/{} completed. Best Fitness: {}",
                              generation, maxGen, population.get(0).getFitness());
                 }


                // Optional: Add termination conditions (e.g., if fitness hasn't improved for N generations, or reaches 0)
                if (population.get(0).getFitness() == 0.0) {
                    log.info("Optimal solution (Fitness 0) found at generation {}.", generation);
                    break; // Stop early if a perfect solution is found
                }
            }

             // Sort final population and return the best
             Collections.sort(population, Comparator.comparingDouble(ScheduleChromosome::getFitness));
             ScheduleChromosome best = population.get(0);
             log.info("GA Run Finished. Final Best Fitness: {}", best.getFitness());
            return best;
        }

        /** Creates the initial population with random assignments. */
        private void initializePopulation(int popSize) {
            log.debug("Initializing population (size: {})...", popSize);
            population.clear();
            for (int i = 0; i < popSize; i++) {
                population.add(createRandomChromosome());
            }
            log.debug("Population initialization complete.");
        }

        /** Creates a single chromosome with random assignments.
         * TODO: Improve initialization to potentially create slightly better starting schedules.
         */
         private ScheduleChromosome createRandomChromosome() {
            List<ShiftAssignment> assignments = new ArrayList<>();
            List<Long> employeeIds = employees.stream().map(Employee::getId).collect(Collectors.toList());

            for (RequiredShiftSlot slot : requiredSlots) {
                Long assignedEmployeeId = null;
                // Simple random assignment: ~80% chance to assign *someone*, otherwise left unassigned (null)
                // TODO: Improve initial assignment: Could bias towards employees who are available and have skills.
                if (!employeeIds.isEmpty() && random.nextDouble() < 0.8) {
                     assignedEmployeeId = employeeIds.get(random.nextInt(employeeIds.size()));

                     // --- Optional: Basic check during initialization ---
                     // Could add simple skill/availability check here to make initial population slightly better.
                     // Employee e = employeeMap.get(assignedEmployeeId); // Need employeeMap here
                     // Task t = slot.task();
                     // if (!e.getSkills().containsAll(t.getRequiredSkills()) ||
                     //     !isEmployeeAvailable(e, slot.date(), slot.startTime(), slot.endTime())) {
                     //    assignedEmployeeId = null; // Revert to unassigned if basic checks fail
                     // }
                     // --- End Optional Check ---
                }
                assignments.add(new ShiftAssignment(slot, assignedEmployeeId));
            }
            return new ScheduleChromosome(assignments);
        }

        /** Calculates fitness for all chromosomes in the population that haven't been evaluated yet. */
        private void evaluatePopulation(List<ScheduleChromosome> populationToEvaluate) {
             log.trace("Starting fitness evaluation for {} chromosomes...", populationToEvaluate.size());
             long evaluatedCount = 0;
             for (ScheduleChromosome chromosome : populationToEvaluate) {
                 // Only calculate fitness if it hasn't been calculated before (fitness < 0)
                 if (chromosome.getFitness() < 0) {
                     chromosome.setFitness(fitnessCalculator.calculateFitness(chromosome));
                     evaluatedCount++;
                 }
             }
             log.trace("Fitness evaluation complete. Calculated fitness for {} chromosomes.", evaluatedCount);
        }

        /** Selects a parent chromosome using Tournament Selection. */
        private ScheduleChromosome tournamentSelection(int tourneySize) {
            ScheduleChromosome best = null;
            // Randomly select TOURNAMENT_SIZE individuals from the population
            for (int i = 0; i < tourneySize; i++) {
                int randomIndex = random.nextInt(population.size());
                ScheduleChromosome candidate = population.get(randomIndex);
                // The candidate with the lowest fitness (best) wins the tournament
                if (best == null || candidate.getFitness() < best.getFitness()) {
                    best = candidate;
                }
            }
            // Return the winner of the tournament
            return best; // Note: Returns a reference to the chromosome in the population
        }

        /** Performs one-point crossover between two parents to create one offspring. */
        private ScheduleChromosome crossover(ScheduleChromosome parent1, ScheduleChromosome parent2) {
             log.trace("Performing crossover between parents (Fitness: {}, {})", parent1.getFitness(), parent2.getFitness());
            // Create a new list for the child's assignments
            List<ShiftAssignment> childAssignments = new ArrayList<>(parent1.size());
            // Choose a random crossover point
            int crossoverPoint = random.nextInt(parent1.size()); // Can be 0 to size-1

            // Copy assignments from parent1 up to the crossover point
            for (int i = 0; i < crossoverPoint; i++) {
                childAssignments.add(parent1.getAssignments().get(i));
            }
            // Copy assignments from parent2 from the crossover point onwards
            for (int i = crossoverPoint; i < parent1.size(); i++) {
                childAssignments.add(parent2.getAssignments().get(i));
            }
            // Create the new offspring chromosome
            return new ScheduleChromosome(childAssignments);
            // TODO: Consider other crossover types (e.g., Uniform Crossover)
        }

        /** Applies mutation to a chromosome by randomly changing some assignments based on mutation rate. */
        private void mutate(ScheduleChromosome chromosome, double mutRate) {
             int mutationCount = 0;
            List<Long> employeeIds = employees.stream().map(Employee::getId).collect(Collectors.toList());

            // Iterate through each assignment (gene) in the chromosome
            for (int i = 0; i < chromosome.size(); i++) {
                // Apply mutation based on the MUTATION_RATE
                if (random.nextDouble() < mutRate) {
                    // Change assignment: Either assign a different random employee or set to unassigned (null)
                    Long currentEmployeeId = chromosome.getAssignments().get(i).employeeId();
                    Long newEmployeeId = null;

                    // Decide whether to assign *any* employee or make it unassigned
                     if (!employeeIds.isEmpty() && random.nextBoolean()) { // 50% chance to try assigning *someone*
                          // Select a random employee ID different from the current one
                          do {
                              newEmployeeId = employeeIds.get(random.nextInt(employeeIds.size()));
                          } while (employeeIds.size() > 1 && Objects.equals(newEmployeeId, currentEmployeeId)); // Ensure it's different if possible
                     }
                     // If the random choice was to not assign, or if the list is empty, newEmployeeId remains null.

                     // Update the assignment in the chromosome
                     chromosome.setAssignment(i, newEmployeeId);
                     mutationCount++;
                     // log.trace("Mutation applied at index {}", i); // Optional logging
                }
            }
            if (mutationCount > 0) {
                 log.trace("Mutation applied to {} genes in chromosome.", mutationCount);
            }
            // TODO: Consider more sophisticated mutation operators if needed (e.g., swap mutations)
        }

    } // End GeneticAlgorithmEngine Inner Class


    // ========================================================================
    // === Existing Helper & Mapper Methods (No changes needed here) ==========
    // ========================================================================

    /**
     * Checks if the employee's availability string indicates they are available
     * for the entire duration of the given shift. Now used by FitnessCalculator.
     * Logs are removed as fitness calculation can be verbose; trace logs added there if needed.
     */
    private boolean isEmployeeAvailable(Employee emp, LocalDate date, LocalTime shiftStart, LocalTime shiftEnd) {
        String availability = emp.getAvailability();
        if (availability == null || availability.isBlank()) {
            return false; // Not available if nothing specified
        }

        DayOfWeek requiredDay = date.getDayOfWeek();
        String[] slots = availability.split(",");

        for (String slot : slots) {
            slot = slot.trim();
            if (slot.isEmpty()) continue;

            String[] parts = slot.split("_");
            if (parts.length == 3) {
                String dayPart = parts[0];
                boolean dayMatches;

                if ("Any".equalsIgnoreCase(dayPart)) {
                    dayMatches = true;
                } else {
                    DayOfWeek availableDay = DAY_ABBREVIATIONS.get(dayPart);
                    if (availableDay == null) continue; // Skip invalid day spec
                    dayMatches = (availableDay == requiredDay);
                }

                if (dayMatches) {
                    try {
                        LocalTime availableStart = LocalTime.parse(parts[1], TIME_FORMATTER);
                        LocalTime availableEnd = LocalTime.parse(parts[2], TIME_FORMATTER);

                        if (availableEnd.isBefore(availableStart) || availableEnd.equals(availableStart)) {
                             continue; // Skip invalid time range
                        }

                        // Check if the availability window FULLY CONTAINS the required shift
                        // Shift:         [shiftStart -------- shiftEnd)
                        // Available: [availableStart -------- availableEnd)
                        if (!shiftStart.isBefore(availableStart) && !shiftEnd.isAfter(availableEnd)) {
                            return true; // Found a matching slot covering the entire shift
                        }
                    } catch (DateTimeParseException e) {
                        // Ignore parsing errors for this slot, try next one
                    }
                }
            }
        }
        return false; // No suitable availability slot found
    }


    /**
     * Calculates a preference score for a given shift based on employee preferences string.
     * Used by FitnessCalculator to apply soft constraint penalties/bonuses.
     * Returns: +1 (Preferred), 0 (Neutral), -1 (Unpreferred/Conflict)
     * Logs removed, handled by FitnessCalculator if needed.
     */
    private int getShiftPreferenceScore(Employee emp, LocalDate date, LocalTime shiftStart, LocalTime shiftEnd) {
         String preferences = emp.getPreferences();
        if (preferences == null || preferences.isBlank()) {
            return PREFERENCE_SCORE_NEUTRAL;
        }

        int score = PREFERENCE_SCORE_NEUTRAL; // Start with neutral

        String[] prefs = preferences.split(";");
        for (String pref : prefs) {
            pref = pref.trim();
            if (pref.isEmpty()) continue;

            String[] parts = pref.split(":", 2);
            if (parts.length == 2) {
                String key = parts[0].trim().toLowerCase();
                String value = parts[1].trim();
                if (value.isEmpty()) continue;

                try {
                    switch (key) {
                        case "unpreferred":
                            if (checkPreferenceRule(value, date, shiftStart, shiftEnd)) {
                                return PREFERENCE_SCORE_UNPREFERRED; // Unpreferred match is a hard stop
                            }
                            break;
                        case "preferred":
                            if (checkPreferenceRule(value, date, shiftStart, shiftEnd)) {
                                score = Math.max(score, PREFERENCE_SCORE_PREFERRED); // Becomes preferred if matches
                            }
                            break;
                        case "unpreferredday":
                            DayOfWeek unpreferredDay = DAY_ABBREVIATIONS.get(value);
                            if (unpreferredDay != null && date.getDayOfWeek() == unpreferredDay) {
                                return PREFERENCE_SCORE_UNPREFERRED; // Unpreferred day match
                            }
                            break;
                        case "preferredday":
                            DayOfWeek preferredDay = DAY_ABBREVIATIONS.get(value);
                            if (preferredDay != null && date.getDayOfWeek() == preferredDay) {
                                score = Math.max(score, PREFERENCE_SCORE_PREFERRED); // Preferred day match
                            }
                            break;
                        // Add other preference keys as needed
                    }
                 } catch (Exception e) {
                     log.warn("Error parsing preference rule value for key '{}', value '{}', employee {}: {}", key, value, emp.getName(), e.getMessage());
                 }
            }
        }
        return score; // Return final score (0 or 1, unless -1 returned earlier)
    }


     /**
      * Helper for preference checking: checks if a shift overlaps with a rule definition.
      * Rule format: "Day_TimeBlock" (e.g., "Mon_Morning", "Tue_1400_1800") or "Day_Any"
      * Returns true if the shift MATCHES the rule's time/day condition.
      * Logs removed.
      */
    private boolean checkPreferenceRule(String rule, LocalDate date, LocalTime shiftStart, LocalTime shiftEnd) {
          String[] parts = rule.split("_");
        // Expects Day or Day_TimeSpec
        if (parts.length < 1 || parts.length > 2 ) {
            return false; // Invalid format
        }

        DayOfWeek ruleDay = DAY_ABBREVIATIONS.get(parts[0]);
        if (ruleDay == null) {
             return false; // Invalid day
        }

        // Check if the shift's day matches the rule's day
        if (date.getDayOfWeek() == ruleDay) {
            String timeRule = (parts.length == 2) ? parts[1].trim() : "Any"; // Default to Any time if only day specified

            if ("Any".equalsIgnoreCase(timeRule)) {
                return true; // Day matches, and any time on that day is specified
            }

            LocalTime ruleStart = null, ruleEnd = null;

            // Try parsing specific time range first (e.g., 1400_1800)
            if (timeRule.contains("_")) {
                String[] timeParts = timeRule.split("_");
                if (timeParts.length == 2) {
                    try {
                        ruleStart = LocalTime.parse(timeParts[0], TIME_FORMATTER);
                        ruleEnd = LocalTime.parse(timeParts[1], TIME_FORMATTER);
                    } catch (DateTimeParseException e) {
                        // Fall through to check named blocks if specific time parsing fails
                    }
                }
            }

            // If specific time not parsed/valid OR rule is a named block (Morning, Afternoon, Evening)
            if (ruleStart == null) {
                switch (timeRule.toLowerCase()) {
                    case "morning": ruleStart = MORNING_START; ruleEnd = MORNING_END; break;
                    case "afternoon": ruleStart = AFTERNOON_START; ruleEnd = AFTERNOON_END; break;
                    case "evening": ruleStart = EVENING_START; ruleEnd = EVENING_END; break;
                    // Add other named blocks if needed
                    default:
                        return false; // Unknown/invalid time rule doesn't match
                }
            }

            // Check for overlap between shift [shiftStart, shiftEnd) and rule time [ruleStart, ruleEnd)
            // Overlap exists if start of one is before end of the other, AND end of one is after start of the other.
            if (ruleStart != null && ruleEnd != null && ruleEnd.isAfter(ruleStart)) {
                boolean overlaps = shiftStart.isBefore(ruleEnd) && shiftEnd.isAfter(ruleStart);
                return overlaps; // True if the shift time overlaps with the rule's time block
            }
        }

        return false; // No match found
    }


    /** Formats duration for logging. */
    private String formatDuration(Duration duration) {
        if (duration == null || duration.isZero()) return "0h 0m";
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return String.format("%dh %dm", hours, minutes);
    }

    /** Maps a Shift entity to a ShiftDTO. */
    private ShiftDTO mapToDTO(Shift shift) {
        ShiftDTO dto = new ShiftDTO();
        dto.setId(shift.getId());
        if (shift.getEmployee() != null) {
            dto.setEmployeeId(shift.getEmployee().getId());
            dto.setEmployeeName(shift.getEmployee().getName());
        } else {
            dto.setEmployeeId(null);
            // Represents an unassigned shift if somehow persisted, or for display logic.
            dto.setEmployeeName("Unassigned");
        }
        if (shift.getTask() != null) {
            dto.setTaskId(shift.getTask().getId());
            dto.setTaskName(shift.getTask().getName());
        } else {
            dto.setTaskId(null);
            dto.setTaskName("No Task Assigned"); // Or null
        }
        dto.setShiftDate(shift.getShiftDate());
        dto.setStartTime(shift.getStartTime());
        dto.setEndTime(shift.getEndTime());
        return dto;
    }

    // Removed findEligibleEmployees method as eligibility is implicitly handled by GA fitness penalties.
    // Removed explicit assignedHours tracking map; fitness function needs to calculate hours/consecutive days etc. from chromosome assignments.

    /**
     * Deletes a single shift by its ID.
     * @param shiftId The ID of the shift to delete.
     * @throws jakarta.persistence.EntityNotFoundException if the shift is not found.
     * @throws RuntimeException if deletion fails for other reasons.
     */
    @Transactional
    public void deleteSingleShift(Long shiftId) {
        log.info("Attempting to delete single shift with ID: {}", shiftId);
        if (!shiftRepository.existsById(shiftId)) {
            throw new jakarta.persistence.EntityNotFoundException("Shift not found with id: " + shiftId);
        }
        try {
            shiftRepository.deleteById(shiftId);
            log.info("Successfully deleted shift with ID: {}", shiftId);
        } catch (Exception e) {
            log.error("Error deleting shift with ID {}: {}", shiftId, e.getMessage(), e);
            // Re-throw runtime exception to ensure transaction rollback
            throw new RuntimeException("Failed to delete shift with ID: " + shiftId, e);
        }
    }

} // End ScheduleService class