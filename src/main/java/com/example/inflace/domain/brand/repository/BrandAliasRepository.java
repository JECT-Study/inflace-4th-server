package com.example.inflace.domain.brand.repository;

import com.example.inflace.domain.brand.domain.BrandAlias;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandAliasRepository extends JpaRepository<BrandAlias, Long> {

    List<BrandAlias> findByAliasIgnoreCaseIn(Collection<String> aliases);
}
