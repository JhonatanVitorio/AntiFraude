package com.antifraude.valores_receber_antifraude_api.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.antifraude.valores_receber_antifraude_api.core.model.entity.BlacklistEntry;

import java.util.List;
import java.util.UUID;

public interface BlacklistRepository extends JpaRepository<BlacklistEntry, UUID> {
    List<BlacklistEntry> findByActiveTrue();
}
