package com.example.inflace.domain.user.domain.entity;

import com.example.inflace.domain.user.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public static UserType of(UserRole role, User user) {
        UserType userType = new UserType();

        userType.role = role;
        userType.user = user;

        return userType;
    }
}
