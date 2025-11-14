package com.antifraude.valores_receber_antifraude_api.features.check.service;

import com.antifraude.valores_receber_antifraude_api.core.model.entity.BlacklistEntry;
import com.antifraude.valores_receber_antifraude_api.core.model.entity.UrlRecord;
import com.antifraude.valores_receber_antifraude_api.core.model.entity.WhitelistEntry;
import com.antifraude.valores_receber_antifraude_api.core.model.enums.Verdict;
import com.antifraude.valores_receber_antifraude_api.core.repository.BlacklistRepository;
import com.antifraude.valores_receber_antifraude_api.core.repository.UrlRecordRepository;
import com.antifraude.valores_receber_antifraude_api.core.repository.WhitelistRepository;
import com.antifraude.valores_receber_antifraude_api.features.check.dto.CheckRequest;
import com.antifraude.valores_receber_antifraude_api.features.check.dto.CheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de integração do fluxo completo de verificação:
 *
 * CheckService.submit(...) -> grava UrlRecord
 * -> alimenta blacklist/whitelist conforme veredito
 */
@SpringBootTest
@Transactional
class CheckServiceIntegrationTest {

    @Autowired
    private CheckService checkService;

    @Autowired
    private UrlRecordRepository urlRecordRepository;

    @Autowired
    private BlacklistRepository blacklistRepository;

    @Autowired
    private WhitelistRepository whitelistRepository;

    @BeforeEach
    void limparBanco() {
        // Ordem para evitar FK issues (se tiver)
        blacklistRepository.deleteAll();
        whitelistRepository.deleteAll();
        urlRecordRepository.deleteAll();
    }

    @Test
    void deveClassificarSuspect_eSalvarEmUrlRecordEBlacklist() {
        // URL claramente suspeita (pelas heurísticas/IA/ThreatIntel que você já
        // configurou)
        String urlSuspeita = "http://simulador-irpf.site";

        CheckRequest req = new CheckRequest();
        // se CheckRequest tiver outros campos (inputType), não faz diferença
        req.setRawInput(urlSuspeita);

        // when
        CheckResponse resp = checkService.submit(req);

        // then – veredito
        assertEquals(Verdict.SUSPECT, resp.getVerdict(), "Veredito deveria ser SUSPECT para URL de golpe");
        assertTrue(resp.getScore() >= 70, "Score de suspeita deveria ser alto");

        // histórico da URL
        Optional<UrlRecord> optRecord = urlRecordRepository.findByNormalizedUrl(resp.getNormalizedUrl());
        assertTrue(optRecord.isPresent(), "UrlRecord deveria ter sido salvo no banco");

        UrlRecord record = optRecord.get();
        assertEquals(Verdict.SUSPECT, record.getLastStatus());
        assertEquals(resp.getScore(), record.getLastScore());

        // entrada na blacklist
        List<BlacklistEntry> blacks = blacklistRepository.findAll();
        assertEquals(1, blacks.size(), "Deveria ter 1 entrada na blacklist após URL suspeita");

        BlacklistEntry entry = blacks.get(0);
        assertTrue(entry.getValue().contains("simulador-irpf.site"),
                "Valor na blacklist deveria conter o domínio da URL suspeita");
        assertTrue(entry.isActive());
    }

    @Test
    void deveClassificarLegit_eSalvarEmUrlRecordEWhitelist() {
        // URL considerada legítima pelas heurísticas/ThreatIntel/IA
        String urlLegit = "https://www.caixa.gov.br";

        CheckRequest req = new CheckRequest();
        req.setRawInput(urlLegit);

        // when
        CheckResponse resp = checkService.submit(req);

        // then – veredito
        assertEquals(Verdict.LEGIT, resp.getVerdict(), "Veredito deveria ser LEGIT para URL confiável");

        // histórico da URL
        Optional<UrlRecord> optRecord = urlRecordRepository.findByNormalizedUrl(resp.getNormalizedUrl());
        assertTrue(optRecord.isPresent(), "UrlRecord deveria ter sido salvo no banco");

        UrlRecord record = optRecord.get();
        assertEquals(Verdict.LEGIT, record.getLastStatus());

        // entrada na whitelist
        List<WhitelistEntry> whites = whitelistRepository.findAll();
        assertEquals(1, whites.size(), "Deveria ter 1 entrada na whitelist após URL legítima");

        WhitelistEntry entry = whites.get(0);
        assertTrue(entry.getValue().contains("caixa.gov.br"),
                "Valor na whitelist deveria conter o domínio da URL legítima");
        assertTrue(entry.isActive());
    }
}
