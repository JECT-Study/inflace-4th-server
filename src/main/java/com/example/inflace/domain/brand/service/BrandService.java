package com.example.inflace.domain.brand.service;

import com.example.inflace.domain.brand.repository.BrandAliasRepository;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandService {

    private final BrandAliasRepository brandAliasRepository;

    public Map<String, String> resolveAliasToNameMap(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Map.of();
        }
        return brandAliasRepository.findByAliasIgnoreCaseIn(tags).stream()
                .collect(Collectors.toMap(
                        ba -> ba.getAlias().toLowerCase(),
                        ba -> ba.getBrand().getName(),
                        (a, b) -> a
                ));
    }
}
