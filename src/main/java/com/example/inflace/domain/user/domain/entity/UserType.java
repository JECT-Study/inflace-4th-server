package com.example.inflace.domain.user.domain.entity;

import com.example.inflace.domain.user.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_name")
    private UserRole typeName;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    public UserType(UserRole typeName, User user) {
        this.typeName = typeName;
        this.user = user;
    }
}