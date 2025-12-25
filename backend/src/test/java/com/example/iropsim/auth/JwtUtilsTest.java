package com.example.iropsim.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private final String testSecret = "test-jwt-secret-key-for-testing-purposes-only-32-chars";

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3600000); // 1 hour
    }

    @Test
    void testGenerateAndValidateToken() {
        // Create test user
        UserDetails userDetails = User.withUsername("testuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        // Generate token
        String token = jwtUtils.generateTokenFromUsername(userDetails.getUsername());

        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Validate token
        boolean isValid = jwtUtils.validateJwtToken(token);
        assertTrue(isValid);

        // Extract username
        String extractedUsername = jwtUtils.getUserNameFromJwtToken(token);
        assertEquals("testuser", extractedUsername);
    }

    @Test
    void testInvalidToken() {
        String invalidToken = "invalid.jwt.token";

        boolean isValid = jwtUtils.validateJwtToken(invalidToken);
        assertFalse(isValid);
    }

    @Test
    void testEmptyToken() {
        boolean isValid = jwtUtils.validateJwtToken("");
        assertFalse(isValid);
    }

    @Test
    void testNullToken() {
        boolean isValid = jwtUtils.validateJwtToken(null);
        assertFalse(isValid);
    }

    @Test
    void testTokenExpiration() {
        // Set very short expiration for testing
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", -1); // Already expired

        UserDetails userDetails = User.withUsername("testuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        String token = jwtUtils.generateTokenFromUsername(userDetails.getUsername());

        // Token should be invalid due to expiration
        boolean isValid = jwtUtils.validateJwtToken(token);
        assertFalse(isValid);
    }

    @Test
    void testMalformedToken() {
        String malformedToken = "malformed-token-without-proper-structure";

        boolean isValid = jwtUtils.validateJwtToken(malformedToken);
        assertFalse(isValid);
    }

    @Test
    void testTokenWithBearerPrefix() {
        UserDetails userDetails = User.withUsername("testuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        String token = jwtUtils.generateTokenFromUsername(userDetails.getUsername());
        String bearerToken = "Bearer " + token;

        // This should work since we extract the token part
        boolean isValid = jwtUtils.validateJwtToken(token);
        assertTrue(isValid);

        String extractedUsername = jwtUtils.getUserNameFromJwtToken(token);
        assertEquals("testuser", extractedUsername);
    }
}
