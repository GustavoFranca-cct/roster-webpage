package com.simpleroster.routegenerator.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;

// import java.util.HashSet; // Not needed if roles are not a Set
// import java.util.Set;

/**
 * Represents a user account for logging into the application.
 * Stores credentials and enabled status.
 * TODO: Enhance with roles/permissions if needed in the future.
 */
@Entity
@Table(name = "users") // Explicit table name
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "username") // Use username for equals/hashCode as it should be unique
@ToString // Basic toString is likely fine here
public class User {

    /** Unique identifier for the user. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The username used for login (must be unique). */
    @Column(nullable = false, unique = true)
    private String username;

    /** The user's hashed password. Should never store plain text passwords. */
    @Column(nullable = false)
    private String password;

    /** Flag indicating if the user account is enabled and can log in. */
    @Column(nullable = false)
    private boolean enabled = true; // Default to true for new users

    // --- Role Management (Future Enhancement) ---
    // If implementing distinct roles (e.g., ADMIN, MANAGER, USER):
    // 1. Create a Role entity (@Entity, id, name)
    // 2. Add a ManyToMany relationship here:
    //    @ManyToMany(fetch = FetchType.EAGER) // Eager fetch roles might be okay for auth checks
    //    @JoinTable( name = "user_roles",
    //                joinColumns = @JoinColumn(name = "user_id"),
    //                inverseJoinColumns = @JoinColumn(name = "role_id"))
    //    private Set<Role> roles = new HashSet<>();
    // 3. Update UserDetailsService to map these roles to GrantedAuthority.
    // --- End Role Management ---

    // Note: Constructor was removed as Lombok @NoArgsConstructor is present.
    // Add custom constructor if specific initialization needed.
    // public User(String username, String password) {
    //     this.username = username;
    //     this.password = password; // Password should be hashed before calling this constructor ideally
    //     this.enabled = true;
    // }
} 