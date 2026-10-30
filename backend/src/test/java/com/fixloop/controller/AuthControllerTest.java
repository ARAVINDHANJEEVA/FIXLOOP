package com.fixloop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixloop.config.GlobalExceptionHandler;
import com.fixloop.domain.AppUser;
import com.fixloop.dto.auth.AuthUserDto;
import com.fixloop.dto.auth.LoginRequest;
import com.fixloop.dto.auth.LoginResponse;
import com.fixloop.repository.AppUserRepository;
import com.fixloop.repository.DepartmentRepository;
import com.fixloop.repository.LocationRepository;
import com.fixloop.repository.StudentProfileRepository;
import com.fixloop.security.JwtService;
import com.fixloop.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AppUserRepository userRepo;

    @Mock
    private StudentProfileRepository studentRepo;

    @Mock
    private DepartmentRepository deptRepo;

    @Mock
    private LocationRepository locationRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authManager;

    private AuthService authService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepo, studentRepo, deptRepo, locationRepo, passwordEncoder, jwtService, authManager);
        ReflectionTestUtils.setField(authService, "expirationMs", 86400000L);
        AuthController authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Login with correct email and wrong password returns 401 Unauthorized")
    void login_WrongPassword_Returns401() throws Exception {
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest request = LoginRequest.builder()
                .email("admin@fixloop.edu")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("Login with non-existent email returns 401 Unauthorized (generic error, no 404)")
    void login_NonExistentEmail_Returns401() throws Exception {
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("User not found"));

        LoginRequest request = LoginRequest.builder()
                .email("nobody@fixloop.edu")
                .password("admin123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("Login with empty body returns 400 Bad Request")
    void login_EmptyBody_Returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("Login with valid credentials returns 200 OK and valid JWT token")
    void login_ValidCredentials_Returns200AndToken() throws Exception {
        AppUser mockUser = AppUser.builder()
                .id(1)
                .name("System Admin")
                .email("admin@fixloop.edu")
                .passwordHash("$2a$10$hashed")
                .role("ADMIN")
                .restrictedAccess(true)
                .avatarInitials("SA")
                .build();

        when(userRepo.findByEmail("admin@fixloop.edu")).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(any(), any())).thenReturn("mocked.jwt.token");

        LoginRequest request = LoginRequest.builder()
                .email("admin@fixloop.edu")
                .password("admin123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked.jwt.token"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("admin@fixloop.edu"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.restrictedAccess").value(true));
    }
}
