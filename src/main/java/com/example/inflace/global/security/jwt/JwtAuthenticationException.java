package com.example.inflace.global.security.jwt;

import com.example.inflace.global.exception.ErrorDefine;
import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

@Getter
public class JwtAuthenticationException extends AuthenticationException {

    private final ErrorDefine error;
    private final String message;

    public JwtAuthenticationException(ErrorDefine errorDefine) {
        this(errorDefine, errorDefine.getMessage());
    }

    public JwtAuthenticationException(ErrorDefine errorDefine, String message) {
        this(errorDefine, message, null);
    }

    public JwtAuthenticationException(ErrorDefine errorDefine, String message, Throwable cause) {
        super(message != null ? message : errorDefine.getMessage(), cause);
        this.error = errorDefine;
        this.message = message != null ? message : errorDefine.getMessage();
    }

    @Override
    public String getMessage() {
        return message;
    }
}
