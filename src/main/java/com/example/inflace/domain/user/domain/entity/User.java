package com.example.inflace.domain.user.domain.entity;

import com.example.inflace.domain.user.domain.enums.Plan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    private String name;

    @Column(name = "profile_image")
    private String profileImage;

    private String email;

    @Column(name = "provider_id", nullable = false, unique = true)
    private String providerId;

    @Enumerated(EnumType.STRING)
    private Plan plan;

    @Builder
    public User(String name, String profileImage, String email,
                String providerId, Plan plan) {
        this.name = name;
        this.profileImage = profileImage;
        this.email = email;
        this.providerId = providerId;
        this.plan = plan;
    }
}