package com.example.inflace.domain.user.domain.entity;

import com.example.inflace.domain.user.domain.enums.Need;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users_need")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNeed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "need_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "need")
    private Need need;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    public UserNeed(Need need, User user) {
        this.need = need;
        this.user = user;
    }
}
