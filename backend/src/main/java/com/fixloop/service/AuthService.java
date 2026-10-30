package com.fixloop.service;

import com.fixloop.domain.*;
import com.fixloop.dto.auth.*;
import com.fixloop.repository.*;
import com.fixloop.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AppUserRepository userRepo;
    private final StudentProfileRepository studentRepo;
    private final DepartmentRepository deptRepo;
    private final LocationRepository locationRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;

    @Value("${security.jwt.expiration-time-ms}")
    private long expirationMs;

    public LoginResponse login(LoginRequest request) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(
            request.getEmail(), request.getPassword()
        ));

        AppUser user = userRepo.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));

        return buildLoginResponse(user);
    }

    @Transactional
    public LoginResponse signup(SignupRequest request) {
        // Validate uniqueness
        if (userRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        if (studentRepo.findByRollNo(request.getRollNo()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Roll number already registered");
        }

        // Generate avatar initials from name
        String name = request.getName().trim();
        String[] parts = name.split("\\s+");
        String initials = parts.length >= 2
            ? String.valueOf(parts[0].charAt(0)).toUpperCase() + String.valueOf(parts[parts.length - 1].charAt(0)).toUpperCase()
            : String.valueOf(parts[0].charAt(0)).toUpperCase();

        // Create base user
        AppUser user = AppUser.builder()
            .name(name)
            .email(request.getEmail().toLowerCase().trim())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role("STUDENT")
            .restrictedAccess(false)
            .avatarInitials(initials)
            .build();
        user = userRepo.save(user);

        // Create student profile
        StudentProfile profile = StudentProfile.builder()
            .userId(user.getId())
            .user(user)
            .rollNo(request.getRollNo().trim().toUpperCase())
            .build();

        if (request.getDepartmentId() != null) {
            deptRepo.findById(request.getDepartmentId()).ifPresent(profile::setDepartment);
        }
        if (request.getDefaultLocationId() != null) {
            locationRepo.findById(request.getDefaultLocationId()).ifPresent(profile::setDefaultLocation);
        }

        studentRepo.save(profile);

        return buildLoginResponse(user);
    }

    private LoginResponse buildLoginResponse(AppUser user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole());
        extraClaims.put("userId", user.getId());
        extraClaims.put("restricted", user.isRestrictedAccess());

        UserDetails userDetails = org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPasswordHash())
            .roles(user.getRole().replace("ROLE_", ""))
            .build();

        String token = jwtService.generateToken(userDetails, extraClaims);

        AuthUserDto authUser = AuthUserDto.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .role(user.getRole())
            .restrictedAccess(user.isRestrictedAccess())
            .avatarInitials(user.getAvatarInitials())
            .build();

        return LoginResponse.builder()
            .token(token)
            .type("Bearer")
            .expiresIn(expirationMs)
            .user(authUser)
            .build();
    }
}

