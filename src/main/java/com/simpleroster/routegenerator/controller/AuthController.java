package com.simpleroster.routegenerator.controller;

import com.simpleroster.routegenerator.dto.AuthRequestDTO;
import com.simpleroster.routegenerator.dto.AuthResponseDTO;
import com.simpleroster.routegenerator.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST Controller handling authentication requests (login, registration, auth check).
 * Base path for all endpoints in this controller is /api/auth.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin // TODO: Configure CORS more restrictively for production environments!
public class AuthController {

    private final AuthService authService;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /**
     * Registers a new user.
     *
     * @param request DTO containing username and password for the new user.
     * @return ResponseEntity with 201 Created on success, 400 Bad Request if username is taken
     *         or input is invalid, or 500 Internal Server Error for other issues.
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody AuthRequestDTO request) {
        log.info("POST /register requested for username: {}", request.getUsername());
        try {
            authService.register(request);
            log.info("User {} registered successfully.", request.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed for {}: {}", request.getUsername(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
             log.error("Unexpected error during registration for username {}:", request.getUsername(), e);
             throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during registration", e);
        }
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request DTO containing username and password.
     * @return ResponseEntity containing the AuthResponseDTO (with JWT token) on success (200 OK),
     *         or 401 Unauthorized if credentials are invalid.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO request) {
        log.info("POST /login requested for username: {}", request.getUsername());
        try {
            AuthResponseDTO response = authService.login(request);
            log.info("User {} logged in successfully.", request.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) { // Catches AuthenticationException (subclass of RuntimeException)
            log.warn("Login failed for username {}: {}", request.getUsername(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"); // Don't expose original exception message
        }
    }

    /**
     * Checks if the current user's JWT token is valid.
     * This endpoint is typically protected by the security filter chain.
     * If the request reaches this method, the token has already been validated.
     *
     * @return ResponseEntity with 200 OK if the token is valid (allowing the request through the filter).
     */
    @GetMapping("/check")
    public ResponseEntity<Void> checkAuth() {
         // This endpoint relies on JwtAuthenticationFilter successfully processing the token.
         // If the filter chain allows the request through, the token is considered valid.
         log.debug("GET /check successful - indicates valid token.");
        return ResponseEntity.ok().build();
    }
} 