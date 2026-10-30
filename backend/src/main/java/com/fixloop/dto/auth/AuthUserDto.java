package com.fixloop.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserDto {
    private Integer id;
    private String name;
    private String email;
    private String role;
    private boolean restrictedAccess;
    private String avatarInitials;
}
