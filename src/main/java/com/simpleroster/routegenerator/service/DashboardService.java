package com.simpleroster.routegenerator.service;

import com.simpleroster.routegenerator.dto.AlertDTO;
import com.simpleroster.routegenerator.dto.DashboardStatsDTO;
import com.simpleroster.routegenerator.entity.Shift;
import com.simpleroster.routegenerator.repository.EmployeeRepository;
import com.simpleroster.routegenerator.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for providing data for the application dashboard.
 * Calculates statistics and generates alerts based on employee and schedule data.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;
    // TODO: Inject other repositories (e.g., TimeOffRequestRepository) when needed for more stats/alerts

    /**
     * Calculates and returns key statistics for the dashboard display.
     *
     * @return DashboardStatsDTO containing calculated statistics.
     */
    public DashboardStatsDTO getDashboardStats() {
        log.info("Calculating dashboard statistics.");
        DashboardStatsDTO stats = new DashboardStatsDTO();

        // 1. Active Employees
        try {
            stats.setActiveEmployees(employeeRepository.countByIsActive(true)); // More efficient count
            log.debug("Active employees count: {}", stats.getActiveEmployees());
        } catch (Exception e) {
            log.error("Error counting active employees: {}", e.getMessage(), e);
            stats.setActiveEmployees(0); // Default on error
        }

        // 2. Scheduled Hours This Week
        try {
            LocalDate today = LocalDate.now();
            // Adjust week start/end based on your business definition (e.g., Sunday or Monday start)
            LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            log.debug("Calculating hours for week: {} to {}", startOfWeek, endOfWeek);

            List<Shift> shiftsThisWeek = shiftRepository.findByShiftDateBetweenOrderByShiftDateAscStartTimeAsc(startOfWeek, endOfWeek);
            double totalHours = shiftsThisWeek.stream()
                    .filter(shift -> shift.getStartTime() != null && shift.getEndTime() != null) // Ensure times are not null
                    .mapToDouble(shift -> {
                        try {
                            return Duration.between(shift.getStartTime(), shift.getEndTime()).toMinutes() / 60.0;
                        } catch (Exception e) {
                            log.warn("Error calculating duration for shift ID {}: {}", shift.getId(), e.getMessage());
                            return 0.0; // Ignore shifts with invalid time ranges
                        }
                    })
                    .sum();
            stats.setHoursThisWeek(Math.round(totalHours * 10.0) / 10.0); // Round to one decimal place
            log.debug("Total hours calculated for the week: {}", stats.getHoursThisWeek());
        } catch (Exception e) {
            log.error("Error calculating total hours this week: {}", e.getMessage(), e);
            stats.setHoursThisWeek(0); // Default to 0 on error
        }

        // 3. Open Shifts (Shifts without an assigned employee)
        try {
            stats.setOpenShifts(shiftRepository.countByEmployeeIsNull());
            log.debug("Open shifts count: {}", stats.getOpenShifts());
        } catch (Exception e) {
             log.error("Error counting open shifts: {}", e.getMessage(), e);
             stats.setOpenShifts(0); // Default to 0 on error
        }

        // 4. Pending Time Off Requests (Placeholder)
        // TODO: Implement this when Time Off Request feature is added
        // Example: stats.setPendingTimeOff(timeOffRequestRepository.countByStatus("PENDING"));
        stats.setPendingTimeOff(0); // Placeholder
        log.debug("Pending time off requests: {} (Placeholder)", stats.getPendingTimeOff());

        log.info("Dashboard statistics calculation complete.");
        return stats;
    }

    /**
     * Generates a list of current alerts or notifications for the dashboard.
     * NOTE: Currently returns placeholder data.
     *
     * @return List of AlertDTO objects.
     */
    public List<AlertDTO> getDashboardAlerts() {
        log.info("Generating dashboard alerts.");
        List<AlertDTO> alerts = new ArrayList<>();

        // --- Placeholder Alerts --- 
        // TODO: Replace with real logic based on business rules, schedule conflicts, upcoming events etc.
        // Example Ideas:
        // - Check for understaffed shifts in the near future.
        // - Check for employees exceeding max hours/consecutive days.
        // - Check for schedule conflicts with time-off requests.
        // - Reminders for upcoming holidays or deadlines.

        // Example placeholder data:
        // alerts.add(new AlertDTO("Potential conflict: Alice Manager scheduled during requested time off (Apr 15).", "warning"));
        // alerts.add(new AlertDTO("Reminder: Finalize next week's roster by Friday EOD.", "info"));

        if (alerts.isEmpty()) {
            log.info("No specific dashboard alerts generated (using placeholders or none).");
            // Optionally add a default message if no other alerts exist
            // alerts.add(new AlertDTO("No critical alerts at this time.", "success"));
        } else {
             log.info("Generated {} dashboard alerts.", alerts.size());
        }

        return alerts;
    }
} 