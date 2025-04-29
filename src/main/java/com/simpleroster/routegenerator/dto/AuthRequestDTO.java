package com.simpleroster.routegenerator.dto;

import lombok.Data;

/**
 * Data Transfer Object (DTO) used for authentication requests (login/registration).
 * Contains user credentials.
 */
@Data
public class AuthRequestDTO {
    /** The username provided by the user. */
    private String username;
    /** The raw password provided by the user. */
    private String password;
} 