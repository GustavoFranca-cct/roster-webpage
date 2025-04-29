package com.simpleroster.routegenerator.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a specific task or duty that needs to be scheduled.
 * Tasks have coverage requirements (min/optimal employees) and may require specific skills.
 */
@Entity
@Table(name = "tasks") // Specify table name
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"requiredSkills"}) // Exclude collections from equals/hashCode
@ToString(exclude = {"requiredSkills"}) // Exclude collections from toString
public class Task {

    /** Unique identifier for the task. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The unique name identifying the task (e.g., "Opening Shift", "Barista", "Stocking"). */
    @Column(nullable = false, unique = true)
    private String name;

    /** Optional description of the task. */
    @Column
    private String description;

    /** Minimum number of employees required to cover this task during a shift slot. */
    @Column(nullable = false)
    private Integer minimumCoverage = 1; // Default: At least 1 person needed

    /** Optimal/ideal number of employees for this task during a shift slot. */
    @Column(nullable = false)
    private Integer optimalCoverage = 1; // Default: Optimal is 1 person

    /**
     * Weight factor applied to penalties for under/over staffing this specific task.
     * Higher values make covering this task correctly more important in the GA fitness calculation.
     */
    @Column(nullable = false)
    private Integer penaltyWeight = 10; // Default weight for under/over coverage penalty

    /**
     * Set of skills required to perform this task.
     * ManyToMany relationship with Skill entity, managed via join table "task_required_skills".
     * FetchType.LAZY is used for performance.
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
            name = "task_required_skills",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> requiredSkills = new HashSet<>();

    // Consider adding a custom constructor if needed.

    /**
     * Helper method to add a required skill to the task and maintain the bidirectional relationship.
     * Should be used instead of directly manipulating the requiredSkills set.
     * @param skill The Skill to add as required.
     */
    public void addRequiredSkill(Skill skill) {
        if (skill != null) {
            this.requiredSkills.add(skill);
            skill.getTasks().add(this);
        }
    }

    /**
     * Helper method to remove a required skill from the task and maintain the bidirectional relationship.
     * Should be used instead of directly manipulating the requiredSkills set.
     * @param skill The Skill to remove.
     */
    public void removeRequiredSkill(Skill skill) {
        if (skill != null) {
            this.requiredSkills.remove(skill);
            skill.getTasks().remove(this);
        }
    }
} 