package com.example.inflace.domain.brandcollaboration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BrandCollaborationDefaultVideoScheduler {

    private final BrandCollaborationService brandCollaborationService;

    @EventListener(ApplicationReadyEvent.class)
    public void initOnStartup() {
        try {
            brandCollaborationService.getDefaultVideos();
            log.info("brandCollaboration default videos cache initialized");
        } catch (RuntimeException e) {
            log.warn("Failed to initialize brandCollaboration default videos cache", e);
        }
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void refresh() {
        try {
            brandCollaborationService.refreshDefaultVideosCache();
            log.info("brandCollaboration default videos cache refreshed");
        } catch (RuntimeException e) {
            log.warn("Failed to refresh brandCollaboration default videos cache", e);
        }
    }
}
