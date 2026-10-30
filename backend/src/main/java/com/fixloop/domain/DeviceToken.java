package com.fixloop.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "device_token")
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "fcm_token", nullable = false, unique = true)
    private String fcmToken;

    @Column(nullable = false)
    @Builder.Default
    private String platform = "ANDROID";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
