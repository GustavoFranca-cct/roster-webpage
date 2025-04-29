package com.simpleroster.routegenerator.service;

import com.simpleroster.routegenerator.dto.AuthRequestDTO;
import com.simpleroster.routegenerator.dto.AuthResponseDTO;
import com.simpleroster.routegenerator.entity.User;
import com.simpleroster.routegenerator.repository.UserRepository;
// Import JwtUtil if using JWT
import com.simpleroster.routegenerator.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling user authentication and registration logic.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    /**
     * Registers a new user in the system.
     * Encodes the password before saving.
     *
     * @param request DTO containing the username and raw password.
     * @throws IllegalArgumentException if the username already exists.
     */
    @Transactional
    public void register(AuthRequestDTO request) {
        log.info("Attempting registration for username: {}", request.getUsername());
        // Check if username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("Registration failed: Username '{}' already exists.", request.getUsername());
            throw new IllegalArgumentException("Username already exists");
        }

        // TODO: Add password complexity validation if required

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        // Encode the password before saving
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setEnabled(true); // Enable user by default
        // TODO: Assign roles if role management is implemented

        userRepository.save(newUser);
        log.info("User '{}' registered successfully.", request.getUsername());
    }

    /**
     * Authenticates a user based on username and password.
     * If successful, generates and returns a JWT.
     *
     * @param request DTO containing the username and password.
     * @return AuthResponseDTO containing the JWT and username.
     * @throws AuthenticationException if authentication fails (e.g., bad credentials).
     * @throws RuntimeException if user is somehow not found after successful authentication (should not happen).
     */
    public AuthResponseDTO login(AuthRequestDTO request) {
        log.info("Attempting login for username: {}", request.getUsername());
        try {
            // Perform authentication using Spring Security's AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            log.info("Authentication successful for user: {}", request.getUsername());

            // If authentication is successful, Spring Security context holds the principal
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // We might need the User entity itself for claims or other info (optional)
            // User user = userRepository.findByUsername(userDetails.getUsername())
            //         .orElseThrow(() -> {
            //              log.error("CRITICAL: User '{}' authenticated but not found in repository!", userDetails.getUsername());
            //              return new RuntimeException("User not found after successful authentication");
            //          });

            // Generate JWT token using the authenticated UserDetails
            String token = jwtUtil.generateToken(userDetails);
            log.debug("Generated JWT token for user: {}", userDetails.getUsername());

            // Return the token and username in the response DTO
            return new AuthResponseDTO(token, userDetails.getUsername());

        } catch (AuthenticationException e) {
            log.warn("Authentication failed for user '{}': {}", request.getUsername(), e.getMessage());
            throw e; // Re-throw the exception to be handled by the controller (e.g., return 401)
        }
    }
} 