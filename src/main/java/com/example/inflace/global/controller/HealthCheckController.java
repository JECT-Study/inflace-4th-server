package com.example.inflace.global.controller;

import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController implements HealthCheckApi {


    @GetMapping("/health-check")
    public ResponseEntity<Void> healthCheck(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        if (authorization != null) {
            throw new ApiException(ErrorDefine.INVALID_HEADER_ERROR);
        }

        return ResponseEntity.ok().build();
    }
}
