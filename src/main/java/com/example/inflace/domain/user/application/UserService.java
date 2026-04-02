package com.example.inflace.domain.user.application;

import com.example.inflace.domain.user.infra.UserCommandRepository;
import com.example.inflace.domain.user.infra.UserReadRepository;
import com.example.inflace.domain.user.presentation.OnboardingRequest;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserReadRepository userReadRepository;
    private final UserCommandRepository userCommandRepository;

    @Transactional
    public long registerIfNotExists(String sub, String name, String email, String profileImage) {
        return userCommandRepository.insertIfNotExists(sub, name, email, profileImage);
    }

    @Transactional
    public void onboarding(long userId, OnboardingRequest request) {

        if (request.role() == null || request.need() == null || request.need().isEmpty()) {
            throw new ApiException(ErrorDefine.ONBOARDING_INVALID_REQUEST);
        }

        userCommandRepository.insertUserType(userId, request.role().name());
        userCommandRepository.bulkInsertNeeds(userId, request.need());
    }
}
