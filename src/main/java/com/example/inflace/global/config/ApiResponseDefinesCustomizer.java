package com.example.inflace.global.config;

import com.example.inflace.global.exception.ApiErrorDefines;
import com.example.inflace.global.exception.ErrorDefine;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ApiResponseDefinesCustomizer implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        ApiErrorDefines annotation = handlerMethod.getMethodAnnotation(ApiErrorDefines.class);
        if (annotation == null) {
            return operation;
        }

        ApiResponses responses = operation.getResponses();

        // 200 성공 응답
//        Schema<?> successSchema = new Schema<>()
//                .addProperty("isSuccess", new Schema<>().type("boolean").example(true))
//                .addProperty("responseDto", new Schema<>().type("object").nullable(true))
//                .addProperty("error", new Schema<>().type("object").nullable(true));
//
//        responses.addApiResponse("200", new ApiResponse()
//                .description("성공")
//                .content(new Content().addMediaType("application/json",
//                        new MediaType().schema(successSchema))));

        // 에러 응답 (ErrorDefine의 httpStatus별 그룹핑)
        Map<Integer, List<ErrorDefine>> grouped = Arrays.stream(annotation.value())
                .collect(Collectors.groupingBy(e -> e.getHttpStatus().value()));

        grouped.forEach((statusCode, errors) -> {
            String description = errors.stream()
                    .map(e -> String.format("- **%s** (`%s`): %s", e.name(), e.getErrorCode(), e.getMessage()))
                    .collect(Collectors.joining("\n"));

            Schema<?> errorSchema = new Schema<>()
                    .addProperty("isSuccess", new Schema<>().type("boolean").example(false))
                    .addProperty("responseDto", new Schema<>().type("object").nullable(true))
                    .addProperty("error", new Schema<>()
                            .addProperty("code", new Schema<>().type("string").example(errors.get(0).getErrorCode()))
                            .addProperty("message", new Schema<>().type("string").example(errors.get(0).getMessage())));

            responses.addApiResponse(String.valueOf(statusCode), new ApiResponse()
                    .description(description)
                    .content(new Content().addMediaType("application/json",
                            new MediaType().schema(errorSchema))));
        });

        return operation;
    }
}
