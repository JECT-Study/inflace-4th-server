package com.example.inflace.domain.user.infra;

import com.example.inflace.domain.user.domain.entity.UserAlarm;
import com.example.inflace.domain.user.domain.enums.AlarmType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAlarmRepository extends JpaRepository<UserAlarm, Long> {

    List<UserAlarm> findAllByUser_Id(UUID userId);

    Optional<UserAlarm> findByUser_IdAndAlarmType(UUID userId, AlarmType alarmType);
}
