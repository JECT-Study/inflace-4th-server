package com.example.inflace.domain.brand.service;

import com.example.inflace.domain.brand.repository.BrandAliasRepository;
import com.example.inflace.global.annotation.ReadOnlyTransactional;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandAliasRepository brandAliasRepository;

    @ReadOnlyTransactional
    public Map<String, String> resolveAliasToNameMap(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Map.of();
        }
        return brandAliasRepository.findByAliasIgnoreCaseIn(tags).stream()
                .collect(Collectors.toMap(
                        ba -> ba.getAlias().toLowerCase(Locale.ROOT),
                        ba -> ba.getBrand().getName(),
                        (a, b) -> {
                            log.error("중복 브랜드 alias 감지: alias에 여러 브랜드가 매핑됨 (first={}, second={})", a, b);
                            return a;
                        }
                ));
    }
}
