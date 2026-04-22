package com.example.inflace.global.security.custom;

import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.ApiFilterErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiFilterErrorResponseWriter apiFilterErrorResponseWriter;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        String message = accessDeniedException != null && accessDeniedException.getMessage() != null
                ? accessDeniedException.getMessage()
                : "Forbidden";

        apiFilterErrorResponseWriter.write(
                request,
                response,
                ErrorDefine.AUTH_FORBIDDEN,
                message
        );
    }
}
