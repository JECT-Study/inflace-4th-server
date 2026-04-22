package com.example.inflace.global.response;

import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.properties.CorsAllowedOriginsProperties;
import com.example.inflace.global.security.custom.CustomCorsHeaderConfigurer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ApiFilterErrorResponseWriter {

    private final ObjectMapper objectMapper;
    private final CorsAllowedOriginsProperties corsAllowedOriginsProperties;

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            ErrorDefine errorDefine,
            String message
    ) throws IOException {
        CustomCorsHeaderConfigurer.setCorsHeader(request, response, corsAllowedOriginsProperties.getOrigins());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setStatus(errorDefine.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        BaseResponse<Void> errorResponse = BaseResponse.<Void>builder()
                .isSuccess(false)
                .responseDto(null)
                .error(new ExceptionResponse(errorDefine, message))
                .build();

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
