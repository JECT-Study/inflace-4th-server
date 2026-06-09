package com.example.inflace.global.response;

import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.exception.GlobalRestExceptionHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 예상하지_못한_예외의_내부_메시지는_응답에_노출하지_않는다() throws Exception {
        Exception exception = new IllegalStateException("database password leaked");

        ResponseEntity<?> response = new GlobalRestExceptionHandler().handleException(exception);
        JsonNode body = objectMapper.valueToTree(response.getBody());

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(body.at("/error/code").asText()).isEqualTo(ErrorDefine.INTERNAL_SERVER_ERROR.getErrorCode());
        assertThat(body.at("/error/message").asText()).isEqualTo(ErrorDefine.INTERNAL_SERVER_ERROR.getMessage());
        assertThat(body.toString()).doesNotContain("database password leaked");
    }

    @Test
    void JSON_변환_예외의_메시지와_원인은_응답에_노출하지_않는다() {
        HttpMessageConversionException exception = new HttpMessageConversionException(
                "request body leaked",
                new IllegalArgumentException("parser detail leaked")
        );

        ResponseEntity<?> response = BaseResponse.toResponseEntity(exception);
        JsonNode body = objectMapper.valueToTree(response.getBody());

        assertThat(body.at("/error/code").asText()).isEqualTo(ErrorDefine.INVALID_ARGUMENT.getErrorCode());
        assertThat(body.at("/error/message").asText()).isEqualTo(ErrorDefine.INVALID_ARGUMENT.getMessage());
        assertThat(body.toString())
                .doesNotContain("request body leaked")
                .doesNotContain("parser detail leaked")
                .doesNotContain("cause");
    }

    @Test
    void API_예외의_정의된_메시지는_응답에_유지한다() {
        ResponseEntity<?> response = BaseResponse.toResponseEntity(new ApiException(ErrorDefine.USER_NOT_FOUND));
        JsonNode body = objectMapper.valueToTree(response.getBody());

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(body.at("/error/code").asText()).isEqualTo(ErrorDefine.USER_NOT_FOUND.getErrorCode());
        assertThat(body.at("/error/message").asText()).isEqualTo(ErrorDefine.USER_NOT_FOUND.getMessage());
    }
}
