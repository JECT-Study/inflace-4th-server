package com.example.inflace.infra.email.outbox.domain;

import com.example.inflace.global.entity.BaseTimeEntity;
import com.example.inflace.infra.email.enums.EmailSendType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_outbox_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailOutboxEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "html_content", nullable = false)
    private String htmlContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_send_type", nullable = false)
    private EmailSendType emailSendType;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", nullable = false)
    private PublishStatus publishStatus;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    public static EmailOutboxEvent of(String email, EmailSendType emailSendType, String htmlContent) {
        EmailOutboxEvent event = new EmailOutboxEvent();
        event.email = email;
        event.emailSendType = emailSendType;
        event.htmlContent = htmlContent;
        event.publishStatus = PublishStatus.PENDING;
        event.attemptCount = 0;
        event.publishedAt = null;
        return event;
    }
}
