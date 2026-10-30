package com.fixloop.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 86400000L);
    }

    @Test
    void testGenerateAndValidateToken() {
        UserDetails userDetails = User.withUsername("admin@fixloop.edu")
                .password("encoded_pass")
                .roles("ADMIN")
                .build();

        String token = jwtService.generateToken(userDetails, Map.of("role", "ADMIN", "userId", 1));
        assertNotNull(token);

        String username = jwtService.extractUsername(token);
        assertEquals("admin@fixloop.edu", username);

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }
}
