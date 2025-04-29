// entity/Employee.java
package com.simpleroster.routegenerator.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an employee within the system.
 * Stores personal details, contract information, availability, preferences,
 * scheduling constraints, and assigned skills.
 */
@Entity
@Table(name = "employees")
// Using @Data is generally discouraged for JPA entities due to potential issues
// with lazy loading and bidirectional relationships in generated equals/hashCode/toString.
// Prefer explicit @Getter, @Setter, @ToString, @EqualsAndHashCode (with exclusions).
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"skills"}) // Exclude collections from equals/hashCode
@ToString(exclude = {"skills"}) // Exclude collections from toString to avoid recursion
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Employee's full name (must be unique). */
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    /** Contracted hours per scheduling period (e.g., per week). */
    @Column(name = "contract_hours")
    private int contractHours;

    /**
     * String representation of employee availability.
     * Format: Comma-separated values like "Day_StartTime_EndTime", e.g., "Mon_0900_1700,Tue_1000_1800".
     * Parsing logic is handled in ScheduleService.
     * TODO: Consider a more structured approach (dedicated table or JSONB).
     */
    @Column(name = "availability", columnDefinition = "text")
    private String availability;

    /**
     * String representation of employee preferences.
     * Format: Semicolon-separated key-value pairs, e.g., "unpreferred:Mon_Morning;preferredDay:Wed".
     * Parsing logic is handled in ScheduleService.
     * TODO: Consider a more structured approach (dedicated table or JSONB).
     */
    @Column(name = "preferences", columnDefinition = "TEXT")
    private String preferences;

    // --- Soft Constraint Parameters (Employee Specific) ---
    /** Maximum number of consecutive days the employee can work. Used as a soft constraint in GA. */
    @Column(name = "max_consecutive_days")
    private Integer maxConsecutiveDays = 5; // Default max 5 days in a row

    /** Minimum number of consecutive days the employee should work (if assigned). Used as a soft constraint. */
    @Column(name = "min_consecutive_days")
    private Integer minConsecutiveDays = 1; // Default min 1 day (can be 0 if not applicable)

    /** Maximum number of weekends the employee can work within a scheduling period. Used as a soft constraint. */
    @Column(name = "max_weekends")
    private Integer maxWeekends = 2; // Default max 2 weekends per typical month/period

    /** Maximum total hours the employee can be scheduled for in a period. Can be null if contractHours is the main limit. */
    @Column(name = "max_total_hours")
    private Integer maxTotalHours;

    /** Minimum total hours the employee should be scheduled for in a period. */
    @Column(name = "min_total_hours")
    private Integer minTotalHours = 0; // Default minimum hours

    /** Penalty weight for violating consecutive day constraints (max/min). Higher values increase avoidance. */
    @Column(name = "consecutive_day_penalty_weight")
    private Integer consecutiveDayPenaltyWeight = 5; // Default weight

    /** Penalty weight for violating maximum weekend constraints. Higher values increase avoidance. */
    @Column(name = "weekend_penalty_weight")
    private Integer weekendPenaltyWeight = 10; // Default weight

    /** Penalty weight for violating total hours constraints (max/min). Higher values increase avoidance. */
    @Column(name = "total_hours_penalty_weight")
    private Integer totalHoursPenaltyWeight = 2; // Default weight
    // --- End Soft Constraint Parameters ---

    /** Flag indicating if the employee is currently active and available for scheduling. */
    @Column(name = "isactive", nullable = false)
    private boolean isActive = true; // Default to active

    /**
     * Set of skills possessed by the employee.
     * ManyToMany relationship with Skill entity, managed via join table "employee_skills".
     * FetchType.LAZY is used for performance.
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "employee_skills",
            joinColumns = @JoinColumn(name = "employee_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> skills = new HashSet<>();

    // Note: A custom constructor was removed as Lombok @NoArgsConstructor is present.
    // If specific initialization logic is needed beyond defaults, a custom constructor can be added.

    /**
     * Helper method to add a skill to the employee and maintain the bidirectional relationship.
     * Should be used instead of directly manipulating the skills set.
     * @param skill The Skill to add.
     */
    public void addSkill(Skill skill) {
        if (skill != null) {
            this.skills.add(skill);
            skill.getEmployees().add(this);
        }
    }

    /**
     * Helper method to remove a skill from the employee and maintain the bidirectional relationship.
     * Should be used instead of directly manipulating the skills set.
     * @param skill The Skill to remove.
     */
    public void removeSkill(Skill skill) {
        if (skill != null) {
            this.skills.remove(skill);
            skill.getEmployees().remove(this);
        }
    }

}