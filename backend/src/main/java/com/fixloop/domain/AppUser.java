package com.fixloop.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @Column(unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    private String role;

    @Column(name = "restricted_access")
    private boolean restrictedAccess;

    @Column(name = "avatar_initials")
    private String avatarInitials;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    private void validateRestrictedAccess() {
        if (restrictedAccess && !"ADMIN".equals(role)) {
            throw new IllegalStateException("restricted_access privilege can only be granted to users with role ADMIN");
        }
    }
}
