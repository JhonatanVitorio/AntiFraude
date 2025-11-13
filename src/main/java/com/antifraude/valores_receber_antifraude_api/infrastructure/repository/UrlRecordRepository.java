package com.antifraude.valores_receber_antifraude_api.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.antifraude.valores_receber_antifraude_api.domain.model.UrlRecord;

import java.util.Optional;
import java.util.UUID;

public interface UrlRecordRepository extends JpaRepository<UrlRecord, UUID> {
    Optional<UrlRecord> findByNormalizedUrl(String normalizedUrl);
}
