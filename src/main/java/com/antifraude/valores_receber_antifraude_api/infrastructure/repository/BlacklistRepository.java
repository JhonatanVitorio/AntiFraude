package com.antifraude.valores_receber_antifraude_api.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.antifraude.valores_receber_antifraude_api.domain.model.BlacklistEntry;

import java.util.List;
import java.util.UUID;

public interface BlacklistRepository extends JpaRepository<BlacklistEntry, UUID> {
    List<BlacklistEntry> findByActiveTrue();
}
