package com.antifraude.valores_receber_antifraude_api.features.check.service;

import com.antifraude.valores_receber_antifraude_api.aiAgent.AiAgentService;
import com.antifraude.valores_receber_antifraude_api.core.model.entity.BlacklistEntry;
import com.antifraude.valores_receber_antifraude_api.core.model.entity.UrlRecord;
import com.antifraude.valores_receber_antifraude_api.core.model.entity.WhitelistEntry;
import com.antifraude.valores_receber_antifraude_api.core.model.enums.ListEntryType;
import com.antifraude.valores_receber_antifraude_api.core.model.enums.Verdict;
import com.antifraude.valores_receber_antifraude_api.core.repository.BlacklistRepository;
import com.antifraude.valores_receber_antifraude_api.core.repository.UrlRecordRepository;
import com.antifraude.valores_receber_antifraude_api.core.repository.WhitelistRepository;
import com.antifraude.valores_receber_antifraude_api.core.rules.RulesEngine;
import com.antifraude.valores_receber_antifraude_api.core.rules.UrlNormalizer;
import com.antifraude.valores_receber_antifraude_api.features.check.dto.CheckRequest;
import com.antifraude.valores_receber_antifraude_api.features.check.dto.CheckResponse;
import com.antifraude.valores_receber_antifraude_api.lists.service.ListsService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço de orquestração do pipeline de verificação de URLs.
 *
 * Responsável por:
 * - Normalizar a URL
 * - Consultar Whitelist / Blacklist
 * - Consultar cache de histórico (UrlRecord)
 * - Executar o motor de regras ({@link RulesEngine})
 * - Chamar ThreatIntel + IA via {@link AiAgentService}
 * - Persistir o histórico e alimentar blacklist/whitelist automaticamente
 */
@Service
public class CheckService {

    private final UrlRecordRepository urlRecordRepository;
    private final RulesEngine rulesEngine;
    private final ListsService listsService;
    private final AiAgentService aiAgentService;
    private final BlacklistRepository blacklistRepository;
    private final WhitelistRepository whitelistRepository;

    public CheckService(
            UrlRecordRepository urlRecordRepository,
            RulesEngine rulesEngine,
            ListsService listsService,
            AiAgentService aiAgentService,
            BlacklistRepository blacklistRepository,
            WhitelistRepository whitelistRepository) {
        this.urlRecordRepository = urlRecordRepository;
        this.rulesEngine = rulesEngine;
        this.listsService = listsService;
        this.aiAgentService = aiAgentService;
        this.blacklistRepository = blacklistRepository;
        this.whitelistRepository = whitelistRepository;
    }

    /**
     * Pipeline principal de verificação:
     *
     * 1) Normalização da URL
     * 2) Whitelist (curto-circuito)
     * 3) Blacklist (curto-circuito)
     * 4) Cache (UrlRecord, se já analisamos essa URL)
     * 5) Motor de Regras local
     * 6) IA + ThreatIntel (VirusTotal + LLM)
     */
    @Transactional
    public CheckResponse submit(CheckRequest request) {

        // 1) Normalizar URL
        UrlNormalizer.Result norm = UrlNormalizer.normalize(request.getRawInput());

        // 2) WHITELIST (prioridade máxima)
        CheckResponse whitelistDecision = handleWhitelist(norm);
        if (whitelistDecision != null) {
            return whitelistDecision;
        }

        // 3) BLACKLIST (prioridade alta)
        CheckResponse blacklistDecision = handleBlacklist(norm);
        if (blacklistDecision != null) {
            return blacklistDecision;
        }

        // 4) CACHE (já temos histórico dessa URL?)
        CheckResponse cacheDecision = handleCache(norm);
        if (cacheDecision != null) {
            return cacheDecision;
        }

        // 5) Motor de regras local (heurísticas estáticas)
        CheckResponse rulesDecision = handleRules(norm);
        if (rulesDecision != null) {
            return rulesDecision;
        }

        // 6) IA + Threat Intel (VirusTotal + LLM)
        return handleAi(norm);
    }

    // ---------- Etapas do pipeline ----------

    /**
     * Passo de Whitelist: se a URL/domínio bater com uma regra da whitelist,
     * já retorna LEGIT e não segue para as próximas etapas.
     */
    private CheckResponse handleWhitelist(UrlNormalizer.Result norm) {
        var white = listsService.matchWhitelist(norm.normalizedUrl, norm.domain);
        if (!white.hit) {
            return null;
        }

        UrlRecord rec = upsertRecord(norm, Verdict.LEGIT, 10); // score baixo para URLs confiáveis
        return buildResp(
                rec,
                "LIST",
                List.of(white.ruleCode),
                List.of("Whitelist: " + white.matchedValue));
    }

    /**
     * Passo de Blacklist: se a URL/domínio bater com uma regra da blacklist,
     * retorna SUSPECT imediatamente.
     */
    private CheckResponse handleBlacklist(UrlNormalizer.Result norm) {
        var black = listsService.matchBlacklist(norm.normalizedUrl, norm.domain);
        if (!black.hit) {
            return null;
        }

        UrlRecord rec = upsertRecord(norm, Verdict.SUSPECT, 90); // score alto para URLs bloqueadas
        return buildResp(
                rec,
                "LIST",
                List.of(black.ruleCode),
                List.of("Blacklist: " + black.matchedValue));
    }

    /**
     * Passo de cache: verifica se já temos histórico para essa URL.
     * Se sim, devolve o último veredito armazenado.
     */
    private CheckResponse handleCache(UrlNormalizer.Result norm) {
        var existingOpt = urlRecordRepository.findByNormalizedUrl(norm.normalizedUrl);
        if (existingOpt.isEmpty()) {
            return null;
        }

        UrlRecord rec = existingOpt.get();
        return buildResp(
                rec,
                "CACHE",
                List.of("CACHE_HIT"),
                List.of("Registro prévio no banco"));
    }

    /**
     * Passo de motor de regras local.
     * Se o motor decidir SUSPECT ou LEGIT, além de retornar o veredito,
     * alimenta também blacklist/whitelist automaticamente.
     */
    private CheckResponse handleRules(UrlNormalizer.Result norm) {
        var ruleResult = rulesEngine.evaluate(norm.normalizedUrl, norm.domain);
        if (ruleResult.verdict == Verdict.UNKNOWN) {
            // Se o motor de regras ficou em dúvida, seguimos o pipeline
            return null;
        }

        UrlRecord rec = upsertRecord(norm, ruleResult.verdict, ruleResult.score);

        // Se as REGRAS classificarem como SUSPECT, já joga na BLACKLIST
        if (ruleResult.verdict == Verdict.SUSPECT) {
            addToBlacklist(norm.normalizedUrl, "Rules engine marcou como suspeita");
        }
        // Se as REGRAS classificarem como LEGIT, joga na WHITELIST
        else if (ruleResult.verdict == Verdict.LEGIT) {
            addToWhitelist(norm.normalizedUrl, "Rules engine confirmou como legítima");
        }

        return buildResp(
                rec,
                "RULES",
                ruleResult.ruleHits,
                ruleResult.evidence);
    }

    /**
     * Passo de IA + ThreatIntel.
     * Esse passo só roda se:
     * - URL não estava em whitelist
     * - URL não estava em blacklist
     * - URL não estava em cache
     * - Motor de regras não decidiu
     */
    private CheckResponse handleAi(UrlNormalizer.Result norm) {
        AiAgentService.Result iaResult = aiAgentService.classify(
                norm.normalizedUrl,
                norm.domain,
                0);

        List<String> hits = new ArrayList<>();
        List<String> evidence = new ArrayList<>();

        if (iaResult.ruleHits != null) {
            hits.addAll(iaResult.ruleHits);
        }
        if (iaResult.evidence != null) {
            evidence.addAll(iaResult.evidence);
        }

        UrlRecord rec = upsertRecord(norm, iaResult.verdict, iaResult.score);

        // Sempre que o veredito final for claro,
        // alimenta listas automaticamente (URL completa)
        if (iaResult.verdict == Verdict.SUSPECT) {
            addToBlacklist(norm.normalizedUrl, "Pipeline IA/ThreatIntel marcou como suspeita");
        } else if (iaResult.verdict == Verdict.LEGIT) {
            addToWhitelist(norm.normalizedUrl, "Pipeline IA/ThreatIntel confirmou como legítima");
        }

        return buildResp(rec, iaResult.source, hits, evidence);
    }

    // ---------- Persistência e helpers ----------

    /**
     * Cria ou atualiza o registro de URL (histórico).
     * Sempre mantém o último veredito, score e data de visualização.
     */
    private UrlRecord upsertRecord(UrlNormalizer.Result norm, Verdict verdict, int score) {
        var recOpt = urlRecordRepository.findByNormalizedUrl(norm.normalizedUrl);
        UrlRecord rec = recOpt.orElseGet(UrlRecord::new);

        rec.setNormalizedUrl(norm.normalizedUrl);
        rec.setDomain(norm.domain);
        rec.setLastSeenAt(LocalDateTime.now());
        rec.setLastStatus(verdict);
        rec.setLastScore(Math.max(0, Math.min(score, 100)));

        return urlRecordRepository.save(rec);
    }

    /**
     * Monta o DTO de resposta exposto pela API.
     */
    private CheckResponse buildResp(UrlRecord rec, String source, List<String> hits, List<String> evidence) {
        CheckResponse resp = new CheckResponse();
        resp.setId(rec.getId());
        resp.setVerdict(rec.getLastStatus());
        resp.setScore(rec.getLastScore());
        resp.setRuleHits(hits);
        resp.setEvidenceSummary(evidence);
        resp.setNormalizedUrl(rec.getNormalizedUrl());
        resp.setDomain(rec.getDomain());
        resp.setSource(source);
        resp.setSubmittedAt(LocalDateTime.now());
        return resp;
    }

    /**
     * Adiciona uma entrada de URL na blacklist.
     * Se já existir (violação de UNIQUE), ignora silenciosamente.
     */
    private void addToBlacklist(String url, String reason) {
        try {
            BlacklistEntry entry = new BlacklistEntry();
            entry.setType(ListEntryType.URL);
            entry.setValue(url);
            entry.setActive(true);
            entry.setReason(reason);
            blacklistRepository.save(entry);
        } catch (DataIntegrityViolationException ex) {
            // Já existe entry_value igual na blacklist -> ignoramos
        }
    }

    /**
     * Adiciona uma entrada de URL na whitelist.
     * Se já existir (violação de UNIQUE), ignora silenciosamente.
     */
    private void addToWhitelist(String url, String reason) {
        try {
            WhitelistEntry entry = new WhitelistEntry();
            entry.setType(ListEntryType.URL);
            entry.setValue(url);
            entry.setActive(true);
            entry.setReason(reason);
            whitelistRepository.save(entry);
        } catch (DataIntegrityViolationException ex) {
            // Já existe entry_value igual na whitelist -> ignoramos
        }
    }
}
