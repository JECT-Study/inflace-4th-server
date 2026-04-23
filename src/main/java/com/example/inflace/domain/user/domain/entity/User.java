package com.example.inflace.domain.user.domain.entity;

import com.example.inflace.domain.user.domain.enums.Plan;
import com.example.inflace.global.entity.SoftDeleteTimeEntity;
import com.example.inflace.global.util.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_users_provider_id",
                columnNames = "provider_id"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SoftDeleteTimeEntity {

    @Id
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID id;

    private String name;

    @Column(name = "profile_image")
    private String profileImage;

    private String email;

    @Column(name = "provider_id", nullable = false, unique = true)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan")
    private Plan plan;

    public static User of(
            String name,
            String profileImage,
            String email,
            String providerId,
            Plan plan
    ) {
        User user = new User();

        user.id = UuidV7Generator.next();
        user.name = name;
        user.profileImage = profileImage;
        user.email = email;
        user.providerId = providerId;
        user.plan = plan;

        return user;
    }
}
