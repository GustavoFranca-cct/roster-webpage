package com.simpleroster.routegenerator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) used for authentication responses.
 * Contains the JWT token and user information after successful authentication.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    /** The JWT token for authenticated requests. */
    private String token; // Or sessionId if using sessions
    /** The authenticated user's username. */
    private String username;
    // Add other user details if needed
} 