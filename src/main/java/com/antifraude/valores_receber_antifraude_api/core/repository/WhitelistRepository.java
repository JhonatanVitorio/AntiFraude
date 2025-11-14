package com.antifraude.valores_receber_antifraude_api.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.antifraude.valores_receber_antifraude_api.core.model.entity.WhitelistEntry;

import java.util.List;
import java.util.UUID;

public interface WhitelistRepository extends JpaRepository<WhitelistEntry, UUID> {
    List<WhitelistEntry> findByActiveTrue();
}
