package com.example.inflace.domain.user.application;

import com.example.inflace.domain.user.infra.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void registerIfNotExists(String sub, String name, String email, String profileImage) {
        userRepository.insertIfNotExists(sub, name, email, profileImage);
    }
}
