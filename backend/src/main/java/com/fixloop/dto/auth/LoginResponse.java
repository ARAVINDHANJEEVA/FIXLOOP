package com.fixloop.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    
    @Builder.Default
    private String type = "Bearer";
    
    private long expiresIn;
    private AuthUserDto user;
}
