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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
     * 2) Whitelist
     * 3) Blacklist
     * 4) Cache (histórico) – se quiser, pode desativar
     * 5) Motor de regras
     * 6) IA + Threat Intel
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
        // Se você quiser manter desativado pra testes, pode comentar esse bloco:
        CheckResponse cacheDecision = handleCache(norm);
        if (cacheDecision != null) {
            return cacheDecision;
        }

        // 5) Motor de regras local
        CheckResponse rulesDecision = handleRules(norm);
        if (rulesDecision != null) {
            return rulesDecision;
        }

        // 6) IA + Threat Intel (VirusTotal + LLM) – só se RULES não decidiram
        return handleAi(norm);
    }

    private CheckResponse handleWhitelist(UrlNormalizer.Result norm) {
        var white = listsService.matchWhitelist(norm.normalizedUrl, norm.domain);
        if (!white.hit) {
            return null;
        }

        UrlRecord rec = upsertRecord(norm, Verdict.LEGIT, 10); // score baixo
        return buildResp(
                rec,
                "LIST",
                List.of(white.ruleCode),
                List.of("Whitelist: " + white.matchedValue));
    }

    private CheckResponse handleBlacklist(UrlNormalizer.Result norm) {
        var black = listsService.matchBlacklist(norm.normalizedUrl, norm.domain);
        if (!black.hit) {
            return null;
        }

        UrlRecord rec = upsertRecord(norm, Verdict.SUSPECT, 90); // score alto
        return buildResp(
                rec,
                "LIST",
                List.of(black.ruleCode),
                List.of("Blacklist: " + black.matchedValue));
    }

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

    private CheckResponse handleRules(UrlNormalizer.Result norm) {
        var ruleResult = rulesEngine.evaluate(norm.normalizedUrl, norm.domain);
        if (ruleResult.verdict == Verdict.UNKNOWN) {
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

    /**
     * Cria ou atualiza o registro de URL (histórico).
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
     * Monta o DTO de resposta para a API.
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

    private void addToBlacklist(String url, String reason) {
        BlacklistEntry entry = new BlacklistEntry();
        entry.setType(ListEntryType.URL);
        entry.setValue(url);
        entry.setActive(true);
        entry.setReason(reason);
        blacklistRepository.save(entry);
    }

    private void addToWhitelist(String url, String reason) {
        WhitelistEntry entry = new WhitelistEntry();
        entry.setType(ListEntryType.URL);
        entry.setValue(url);
        entry.setActive(true);
        entry.setReason(reason);
        whitelistRepository.save(entry);
    }
}
