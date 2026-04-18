package com.example.inflace.global.security.util;

import com.example.inflace.global.config.AuthUser;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UUID getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ApiException(ErrorDefine.AUTHENTICATION_FAILED);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthUser authUser) {
            return authUser.userId();
        }

        throw new ApiException(ErrorDefine.AUTHENTICATION_FAILED);
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
