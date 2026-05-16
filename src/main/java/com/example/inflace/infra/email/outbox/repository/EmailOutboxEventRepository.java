package com.example.inflace.infra.email.outbox.repository;

import com.example.inflace.infra.email.outbox.domain.EmailOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailOutboxEventRepository extends JpaRepository<EmailOutboxEvent, Long> {
}
