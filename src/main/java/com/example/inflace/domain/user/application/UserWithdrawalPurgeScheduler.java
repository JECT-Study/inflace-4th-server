package com.example.inflace.domain.user.application;

import com.example.inflace.domain.user.infra.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserWithdrawalPurgeScheduler {

    private final UserCommandRepository userCommandRepository;

    @Value("${user.withdrawal.purge.retention-days:30}")
    private int retentionDays;

    @Transactional
    @Scheduled(cron = "${user.withdrawal.purge.cron}", zone = "Asia/Seoul")
    public void purge() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        int deleted = userCommandRepository.purgeWithdrawnUsers(threshold);
        log.info("userWithdrawalPurge completed. deleted={}, threshold={}", deleted, threshold);
    }
}
