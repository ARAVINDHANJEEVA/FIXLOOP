package com.fixloop.domain;

import jakarta.persistence.*;
import lombok.*;

import org.springframework.data.domain.Persistable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "admin_profile")
public class AdminProfile implements Persistable<Integer> {
    @Id
    @Column(name = "user_id")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "authority_level")
    private Short authorityLevel;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public Integer getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
