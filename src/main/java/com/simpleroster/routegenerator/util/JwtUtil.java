package com.simpleroster.routegenerator.util; // Create util package

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for handling JSON Web Tokens (JWT).
 * Provides methods for generating, extracting claims from, and validating JWTs.
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration.ms}")
    private long expirationTimeMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    // --- Public Methods ---

    /**
     * Extracts the username (subject) from the JWT token.
     *
     * @param token The JWT token string.
     * @return The username contained in the token's subject claim.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from the JWT token.
     *
     * @param token The JWT token string.
     * @return The expiration date.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from the token using a provided claims resolver function.
     *
     * @param token          The JWT token string.
     * @param claimsResolver A function that takes Claims and returns the desired claim value.
     * @param <T>            The type of the claim being extracted.
     * @return The extracted claim value.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a JWT token for the given UserDetails.
     * The username is used as the subject of the token.
     *
     * @param userDetails The UserDetails object representing the authenticated user.
     * @return The generated JWT token string.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        log.debug("Generating token for user: {}", userDetails.getUsername());
        return createToken(claims, userDetails.getUsername());
    }

    /**
     * Validates the JWT token against the provided UserDetails.
     * Checks if the username matches and if the token has not expired.
     *
     * @param token       The JWT token string.
     * @param userDetails The UserDetails object for the user.
     * @return true if the token is valid for the user, false otherwise.
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);
            log.trace("Token validation for user '{}': Username match = {}, Not expired = {}",
                      username, username.equals(userDetails.getUsername()), !isTokenExpired(token));
            return isValid;
        } catch (Exception e) {
            log.warn("Token validation failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    // --- Private Helper Methods ---

    /**
     * Parses the JWT token and extracts all claims.
     *
     * @param token The JWT token string.
     * @return The Claims object containing all data from the token payload.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Checks if the token's expiration date is before the current time.
     *
     * @param token The JWT token string.
     * @return true if the token is expired, false otherwise.
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Creates the JWT string with the specified claims, subject, and expiration.
     *
     * @param claims  A map of custom claims to include in the payload.
     * @param subject The subject of the token (typically the username).
     * @return The compacted JWT string.
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expirationTimeMs);
        log.trace("Creating token for subject '{}' with expiration: {}", subject, expirationDate);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
} 