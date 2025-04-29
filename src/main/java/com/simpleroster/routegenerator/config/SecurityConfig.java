package com.simpleroster.routegenerator.config; // Create a config package if needed

import com.simpleroster.routegenerator.repository.UserRepository;
import com.simpleroster.routegenerator.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // For CSRF disabling
import org.springframework.security.config.http.SessionCreationPolicy; // For stateless
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // For JWT filter later
import org.springframework.http.HttpMethod; // Import HttpMethod
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Spring Security configuration class.
 * Configures authentication, authorization rules, password encoding, CORS, and JWT filter integration.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor // Use Lombok for constructor injection
public class SecurityConfig {

    private final UserRepository userRepository;
    private final JwtAuthenticationFilter jwtAuthFilter;

    /**
     * Configures the main security filter chain.
     * Defines authorization rules for different endpoints, disables CSRF, sets session management to stateless,
     * integrates the JWT authentication filter, configures CORS, and sets the authentication provider.
     *
     * @param http HttpSecurity object to configure.
     * @param authenticationProvider The configured authentication provider.
     * @return The configured SecurityFilterChain.
     * @throws Exception If configuration fails.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationProvider authenticationProvider) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF (common for SPAs with token auth)
                .authorizeHttpRequests(auth -> auth
                        // Allow CORS preflight requests (OPTIONS)
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers(
                                "/", // Landing page
                                "/index.html",
                                "/login.html",
                                "/dashboard.html", // Allow access to the page itself
                                "/roster.html",    // Allow access to the page itself
                                "/css/**",         // Static resources
                                "/js/**",
                                "/images/**",
                                "/site.webmanifest",
                                "/favicon.ico",
                                "/api/auth/login",  // Public auth endpoints
                                "/api/auth/register"
                        ).permitAll()
                        // Explicitly allow GA visualization endpoints
                        .requestMatchers("/api/ga/start").authenticated()
                        .requestMatchers("/api/ga/status").authenticated()
                        // Explicitly allow authenticated access to settings
                        .requestMatchers("/api/settings/**").authenticated()
                        // Secure other API endpoints - require authentication
                        .requestMatchers("/api/**").authenticated()
                        // Deny any other requests not explicitly permitted or matched above.
                        // Use denyAll() for a default-deny policy, safer than just authenticated().
                        .anyRequest().permitAll()
                )
                // Set session management to STATELESS because we are using JWT tokens
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Set the custom authentication provider
                .authenticationProvider(authenticationProvider)

                // Add the JWT filter before the standard UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // Enable CORS using the default configuration or customize further if needed
                .cors(withDefaults());

        return http.build();
    }

    /**
     * Defines the UserDetailsService bean.
     * Fetches user details from the UserRepository based on the username.
     * Maps the application's User entity to Spring Security's UserDetails.
     *
     * @return UserDetailsService implementation.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .map(user -> org.springframework.security.core.userdetails.User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword()) // Password from DB (should be encoded)
                        .disabled(!user.isEnabled()) // Map enabled status
                        // TODO: Map roles/authorities properly if they are implemented in the User entity
                        .roles("USER") // Assigning default role - customize as needed
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    /**
     * Defines the PasswordEncoder bean.
     * Uses BCrypt for strong password hashing.
     *
     * @return PasswordEncoder instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Defines the AuthenticationProvider bean.
     * Uses DaoAuthenticationProvider which leverages the UserDetailsService and PasswordEncoder.
     *
     * @return AuthenticationProvider instance.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Exposes the AuthenticationManager as a bean.
     * Required by components that need to perform authentication explicitly (e.g., AuthService for login).
     *
     * @param config AuthenticationConfiguration provided by Spring.
     * @return AuthenticationManager instance.
     * @throws Exception If configuration fails.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Defines the central CORS configuration for the application.
     * Replaces the need for @CrossOrigin annotations on controllers.
     *
     * @return CorsConfigurationSource bean.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // IMPORTANT: Replace with your actual frontend origin(s) in production!
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:8080", // Example: Local dev frontend
                "https://sailpocket.com" // Example: Production frontend - USE HTTPS
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Cache preflight response for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply this configuration to all paths under /api/
        source.registerCorsConfiguration("/api/**", configuration);
        // You might need to add other paths if your frontend calls non-/api endpoints
        
        return source;
    }

    // TODO: Consider adding a dedicated CORS configuration bean (WebMvcConfigurer) for more fine-grained control
    // if the default .cors(withDefaults()) is not sufficient for production.
    // Example:
    // @Bean
    // public WebMvcConfigurer corsConfigurer() {
    //     return new WebMvcConfigurer() {
    //         @Override
    //         public void addCorsMappings(CorsRegistry registry) {
    //             registry.addMapping("/api/**")
    //                   .allowedOrigins("http://your-frontend-domain.com") // Specify allowed origin
    //                   .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
    //                   .allowedHeaders("*")
    //                   .allowCredentials(true);
    //         }
    //     };
    // }
}