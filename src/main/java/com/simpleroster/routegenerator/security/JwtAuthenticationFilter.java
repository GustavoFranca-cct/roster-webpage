package com.simpleroster.routegenerator.security;

import com.simpleroster.routegenerator.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Spring Security filter that intercepts incoming requests to validate JWT tokens.
 * If a valid JWT is found in the Authorization header, it extracts the username,
 * loads the corresponding UserDetails, validates the token, and sets the authentication
 * in the SecurityContextHolder.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    // UserDetailsService is marked @Lazy to resolve potential circular dependencies during startup
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, @Lazy UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Performs the JWT validation for each incoming request.
     * Checks for the "Authorization: Bearer <token>" header.
     * If a valid token is found and the user is not already authenticated,
     * it sets the authentication context for the request.
     *
     * @param request     The incoming HttpServletRequest.
     * @param response    The outgoing HttpServletResponse.
     * @param filterChain The filter chain to pass the request along.
     * @throws ServletException If an error occurs during filtering.
     * @throws IOException      If an I/O error occurs during filtering.
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        log.trace("Processing request for: {} with method: {}", request.getRequestURI(), request.getMethod());

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 1. Check if Authorization header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.trace("No JWT Bearer token found in Authorization header for {}. Passing request down the chain.", request.getRequestURI());
            filterChain.doFilter(request, response); // Continue chain if no token
            return;
        }

        // 2. Extract the JWT token
        jwt = authHeader.substring(7);
        log.trace("Extracted JWT from header for {}: [JWT length={}]", request.getRequestURI(), jwt.length()); // Avoid logging the full token

        try {
            // 3. Extract username from the token
            username = jwtUtil.extractUsername(jwt);
            log.trace("Extracted username '{}' from JWT for {}", username, request.getRequestURI());

            // 4. Check if username is extracted and user is not already authenticated in this request context
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.trace("User '{}' not authenticated yet for {}, loading UserDetails.", username, request.getRequestURI());

                // 5. Load UserDetails from UserDetailsService
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                log.trace("Loaded UserDetails for user '{}': authorities={}", username, userDetails.getAuthorities());

                // 6. Validate the token against the loaded UserDetails
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    log.debug("JWT token is valid for user '{}' accessing {}. Setting authentication.", username, request.getRequestURI());

                    // 7. Create an authentication token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // Credentials (password) not needed post-authentication
                            userDetails.getAuthorities()
                    );
                    // Set additional details (like IP address, session ID) from the request
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // 8. Set the authentication in the Spring Security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.trace("Authentication set for user '{}' in SecurityContextHolder.", username);
                } else {
                    log.warn("JWT token validation failed for user '{}' accessing {}.", username, request.getRequestURI());
                    // Optionally clear context if invalid token should block, though filter chain proceeds by default
                    // SecurityContextHolder.clearContext();
                }
            } else {
                log.trace("Username is null or user '{}' is already authenticated for {}. Skipping authentication setting.", username, request.getRequestURI());
            }
        } catch (Exception e) {
            // Log exceptions during JWT processing (e.g., expired token, malformed token)
            log.error("Error processing JWT token for {}: {} - {}", request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());
            // Consider clearing context here as well if token errors should prevent access
            // SecurityContextHolder.clearContext();
        }

        // 9. Continue the filter chain
        filterChain.doFilter(request, response);
    }
} 