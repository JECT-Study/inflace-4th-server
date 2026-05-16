package com.example.inflace.infra.email.service;

import com.example.inflace.infra.email.enums.EmailSendType;
import com.example.inflace.infra.email.outbox.domain.EmailOutboxEvent;
import com.example.inflace.infra.email.outbox.repository.EmailOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailOutboxEventService {

    private final SpringTemplateEngine templateEngine;
    private final EmailOutboxEventRepository emailOutboxEventRepository;

    @Transactional
    public void saveEmailOutboxEvent(
            EmailSendType sendType,
            String email,
            Map<String, Object> variablesMap
    ) {
        String htmlContent = buildEmailHtmlContent(sendType, variablesMap);

        EmailOutboxEvent event = EmailOutboxEvent.of(email, sendType, htmlContent);
        emailOutboxEventRepository.save(event);
    }

    private String buildEmailHtmlContent(EmailSendType sendType, Map<String, Object> variablesMap) {
        Context context = new Context();
        context.setVariables(variablesMap);

        return templateEngine.process(sendType.getTemplatePath(), context);
    }

    // 사용 예시:
    // 1. EmailSendType enum에 메일 제목과 Thymeleaf 템플릿 경로를 추가한다.
    //    예: EXAMPLE("예시 이메일", "emails/example-email")
    // 2. src/main/resources/templates/{templatePath}.html 파일을 만든다.
    //    예: src/main/resources/templates/emails/example-email.html
    // 3. 템플릿에서 사용할 변수를 Map에 담아 saveEmailOutboxEvent(...)를 호출한다.
    // 4. API 서버는 email_outbox_event 테이블에 PENDING 상태로 저장만 하고, 실제 발송은 Batch 서버가 처리한다.
    private void saveExampleEmailOutboxEvent(String email) {
        saveEmailOutboxEvent(
                EmailSendType.EXAMPLE,
                email,
                Map.of(
                        "title", "Inflace 예시 이메일",
                        "name", "홍길동",
                        "message", "Transactional Outbox Pattern으로 저장된 이메일 예시입니다.",
                        "description", "API 서버는 이메일 발송 대신 outbox 이벤트 저장만 담당합니다.",
                        "actionUrl", "https://www.inflace.site",
                        "actionText", "Inflace 바로가기"
                )
        );
    }
}
