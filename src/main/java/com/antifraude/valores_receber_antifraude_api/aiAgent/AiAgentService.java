package com.antifraude.valores_receber_antifraude_api.aiAgent;

import com.antifraude.valores_receber_antifraude_api.core.model.enums.Verdict;
import com.antifraude.valores_receber_antifraude_api.core.threatintel.ThreatIntelService;
import com.antifraude.valores_receber_antifraude_api.core.threatintel.ThreatIntelService.Reputation;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Serviço que unifica Threat Intelligence (VirusTotal + heurísticas locais)
 * e IA externa (LLM) para classificação de URLs.
 */
@Service
public class AiAgentService {

    private static final int MALICIOUS_BASE_SCORE = 90;
    private static final int CLEAN_MAX_RULES_SCORE = 30;
    private static final int CLEAN_MIN_SCORE = 10;
    private static final int IA_MIN_SUSPECT_SCORE = 75;
    private static final double IA_PHISHING_THRESHOLD = 0.60;
    private static final double IA_CLEAN_THRESHOLD = 0.40;

    private final ThreatIntelService threatIntelService;
    private final ExternalAiClient externalAiClient;

    public AiAgentService(
            ThreatIntelService threatIntelService,
            ExternalAiClient externalAiClient) {
        this.threatIntelService = threatIntelService;
        this.externalAiClient = externalAiClient;
    }

    /**
     * Resultado unificado da IA (Threat Intel + LLM).
     */
    public static class Result {
        public final Verdict verdict;
        public final int score;
        public final String source; // "IA", "THREAT_INTEL"
        public final List<String> ruleHits;
        public final List<String> evidence;

        public Result(
                Verdict verdict,
                int score,
                String source,
                List<String> ruleHits,
                List<String> evidence) {
            this.verdict = verdict;
            this.score = score;
            this.source = source;
            this.ruleHits = ruleHits;
            this.evidence = evidence;
        }
    }

    /**
     * Pipeline:
     * 1) Threat Intel (VirusTotal + heurísticas locais)
     * 2) IA (OpenAI / LLM)
     */
    public Result classify(String normalizedUrl, String domain, int rulesScoreBase) {
        List<String> hits = new ArrayList<>();
        List<String> evidence = new ArrayList<>();

        // 1) Threat Intelligence
        ThreatIntelService.Result ti = threatIntelService.check(normalizedUrl, domain);

        if (ti != null) {
            if (ti.getRuleHits() != null) {
                hits.addAll(ti.getRuleHits());
            }
            if (ti.getEvidence() != null) {
                evidence.addAll(ti.getEvidence());
            }
        }

        Result tiDecision = decideByThreatIntel(ti, rulesScoreBase, hits, evidence);
        if (tiDecision != null) {
            return tiDecision;
        }

        String mainEvidenceSummary = evidence.isEmpty()
                ? "Sem evidências fortes de Threat Intel."
                : String.join(" | ", evidence);

        // 2) IA externa (LLM)
        ExternalAiResponse aiResp = externalAiClient.classify(
                normalizedUrl,
                domain,
                rulesScoreBase,
                mainEvidenceSummary);

        return decideByAi(aiResp, rulesScoreBase, hits, evidence);
    }

    /**
     * Decide com base apenas na Threat Intel.
     * Retorna null se não for conclusivo (UNKNOWN), permitindo seguir para a IA.
     */
    private Result decideByThreatIntel(
            ThreatIntelService.Result ti,
            int rulesScoreBase,
            List<String> hits,
            List<String> evidence) {
        if (ti == null || ti.getReputation() == null) {
            return null;
        }

        Reputation rep = ti.getReputation();
        evidence.add("Threat Intel reputação: " + rep.name());

        // Malicioso com base em Threat Intel
        if (rep == Reputation.MALICIOUS) {
            int score = Math.max(rulesScoreBase, MALICIOUS_BASE_SCORE);
            hits.add("THREAT_INTEL_MALICIOUS");
            return new Result(
                    Verdict.SUSPECT,
                    score,
                    "THREAT_INTEL",
                    hits,
                    evidence);
        }

        // Limpo com base em Threat Intel e regras fracas
        if (rep == Reputation.CLEAN && rulesScoreBase < CLEAN_MAX_RULES_SCORE) {
            int score = Math.min(rulesScoreBase, CLEAN_MIN_SCORE);
            hits.add("THREAT_INTEL_CLEAN");
            return new Result(
                    Verdict.LEGIT,
                    score,
                    "THREAT_INTEL",
                    hits,
                    evidence);
        }

        // UNKNOWN → apenas registrar, deixar seguir para IA
        if (rep == Reputation.UNKNOWN) {
            hits.add("THREAT_INTEL_UNKNOWN");
        }

        return null;
    }

    /**
     * Decide com base apenas na IA externa (LLM).
     */
    private Result decideByAi(
            ExternalAiResponse aiResp,
            int rulesScoreBase,
            List<String> hits,
            List<String> evidence) {
        if (aiResp == null) {
            evidence.add("IA externa indisponível; mantendo UNKNOWN.");
            hits.add("IA_ERROR");
            return new Result(
                    Verdict.UNKNOWN,
                    rulesScoreBase,
                    "IA",
                    hits,
                    evidence);
        }

        double riskScore = aiResp.getRiskScore() != null ? aiResp.getRiskScore() : 0.0;
        boolean phishing = aiResp.getPhishing() != null && aiResp.getPhishing();

        int finalScore = (int) Math.round(Math.max(riskScore * 100, rulesScoreBase));

        if (aiResp.getExplanation() != null && !aiResp.getExplanation().isBlank()) {
            evidence.add("IA: " + aiResp.getExplanation());
        }

        // SUSPECT pela IA
        if (phishing && riskScore >= IA_PHISHING_THRESHOLD) {
            hits.add("IA_PHISHING");
            return new Result(
                    Verdict.SUSPECT,
                    Math.max(finalScore, IA_MIN_SUSPECT_SCORE),
                    "IA",
                    hits,
                    evidence);
        }

        // LEGÍTIMA pela IA
        if (!phishing && riskScore <= IA_CLEAN_THRESHOLD) {
            hits.add("IA_CLEAN");
            return new Result(
                    Verdict.LEGIT,
                    finalScore,
                    "IA",
                    hits,
                    evidence);
        }

        // INCONCLUSIVA
        hits.add("IA_INCONCLUSIVE");
        evidence.add("IA não teve confiança suficiente para classificação final.");

        return new Result(
                Verdict.UNKNOWN,
                finalScore,
                "IA",
                hits,
                evidence);
    }
}
