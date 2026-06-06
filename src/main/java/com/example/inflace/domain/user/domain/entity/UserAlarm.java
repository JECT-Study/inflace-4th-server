package com.example.inflace.domain.user.domain.entity;

import com.example.inflace.domain.user.domain.enums.AlarmType;
import com.example.inflace.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "user_alarm",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_alarm_user_alarm_type",
                columnNames = {"user_id", "alarm_type"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAlarm extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_alarm_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "alarm_type", nullable = false, length = 50)
    private AlarmType alarmType;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    public static UserAlarm of(User user, AlarmType alarmType, boolean enabled) {
        UserAlarm userAlarm = new UserAlarm();

        userAlarm.user = user;
        userAlarm.alarmType = alarmType;
        userAlarm.enabled = enabled;

        return userAlarm;
    }

    public void updateEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
