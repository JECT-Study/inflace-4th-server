package com.example.inflace.domain.auth.application;

import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuthStrategyRouter {

    private final Map<String, OAuthLoginStrategy> strategyMap;

    public OAuthLoginStrategy getStrategy(String provider) {
        OAuthLoginStrategy strategy = strategyMap.get(provider);
        if (strategy == null) {
            throw new ApiException(ErrorDefine.AUTH_UNSUPPORTED_PROVIDER);
        }
        return strategy;
    }
}
