package com.simpleroster.routegenerator.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a skill that can be possessed by employees and required by tasks.
 * Used for matching employees to tasks during schedule generation.
 */
@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
// Custom equals/hashCode based on the unique 'name' field is appropriate here.
// Exclude collections from generated implementations.
@EqualsAndHashCode(of = "name")
@ToString(exclude = {"employees", "tasks"})
public class Skill {

    /** Unique identifier for the skill. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The unique name of the skill (e.g., "Barista", "Cash Handling", "Shift Lead").
     * This is used as the business key.
     */
    @Column(nullable = false, unique = true)
    private String name;

    // Consider adding description, category etc. if needed

    /** Set of employees who possess this skill. MappedBy refers to the 'skills' field in Employee. */
    @ManyToMany(mappedBy = "skills", fetch = FetchType.LAZY)
    private Set<Employee> employees = new HashSet<>();

    /** Set of tasks that require this skill. MappedBy refers to the 'requiredSkills' field in Task. */
    @ManyToMany(mappedBy = "requiredSkills", fetch = FetchType.LAZY)
    private Set<Task> tasks = new HashSet<>();

    /**
     * Constructor for creating a Skill with a name.
     * @param name The name of the skill.
     */
    public Skill(String name) {
        this.name = name;
    }

    // Note: Custom equals() and hashCode() based on 'name' were already implemented.
    // Lombok's @EqualsAndHashCode(of = "name") achieves the same.
    // @Override
    // public boolean equals(Object o) {
    //     if (this == o) return true;
    //     // Use instanceof check for proxy safety
    //     if (!(o instanceof Skill)) return false;
    //     Skill skill = (Skill) o;
    //     // Check for null name although it's marked as non-nullable
    //     return name != null && name.equals(skill.name);
    // }

    // @Override
    // public int hashCode() {
    //     // Use name's hashCode, handle null although non-nullable
    //     return name != null ? name.hashCode() : 0;
    // }
} 