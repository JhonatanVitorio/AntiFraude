package com.antifraude.valores_receber_antifraude_api.aiAgent;

import com.antifraude.valores_receber_antifraude_api.core.model.enums.Verdict;
import com.antifraude.valores_receber_antifraude_api.core.threatintel.ThreatIntelService;
import com.antifraude.valores_receber_antifraude_api.core.threatintel.ThreatIntelService.Reputation;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Serviço responsável por orquestrar a decisão final de risco da URL,
 * combinando:
 * 
 * Threat Intelligence (VirusTotal + heurísticas locais) 
 * IA externa (LLM) simulada via {@link ExternalAiClient} 
 *
 * A ideia é:
 * Tentar decidir apenas com Threat Intel (casos muito limpos ou muito
 * sujos) 
 * Se Threat Intel for inconclusivo, pedir uma segunda opinião da IA 
 */
@Service
public class AiAgentService {

    // Pontuações e thresholds para calibrar a decisão final
    private static final int MALICIOUS_BASE_SCORE = 90; // score mínimo para algo ser bem suspeito
    private static final int CLEAN_MAX_RULES_SCORE = 30; // se regras forem fracas e TI disser CLEAN, consideramos
                                                         // legítimo
    private static final int CLEAN_MIN_SCORE = 10; // score máximo para algo bem limpo
    private static final int IA_MIN_SUSPECT_SCORE = 75; // score mínimo quando a IA identificar phishing
    private static final double IA_PHISHING_THRESHOLD = 0.60; // a partir de 60% de risco a IA considera phishing
    private static final double IA_CLEAN_THRESHOLD = 0.40; // abaixo de 40% de risco a IA considera limpo

    private final ThreatIntelService threatIntelService;
    private final ExternalAiClient externalAiClient;

    public AiAgentService(
            ThreatIntelService threatIntelService,
            ExternalAiClient externalAiClient) {
        this.threatIntelService = threatIntelService;
        this.externalAiClient = externalAiClient;
    }

    /**
     * Objeto de retorno do pipeline de IA.
     * Representa o "veredito unificado" após ThreatIntel + IA.
     */
    public static class Result {
        public final Verdict verdict; // SUSPECT / LEGIT / UNKNOWN
        public final int score; // 0 a 100
        public final String source; // "IA" ou "THREAT_INTEL" (quem decidiu)
        public final List<String> ruleHits; // códigos de regras que dispararam
        public final List<String> evidence; // evidências textuais

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
     * Pipeline principal de classificação da IA.
     *
     * @param normalizedUrl  URL normalizada
     * @param domain         domínio extraído da URL
     * @param rulesScoreBase score base vindo do motor de regras (se houver)
     *
     * @return {@link Result} com veredito, score, fonte e evidências
     */
    public Result classify(String normalizedUrl, String domain, int rulesScoreBase) {
        List<String> hits = new ArrayList<>();
        List<String> evidence = new ArrayList<>();

        // 1) Threat Intelligence 
        ThreatIntelService.Result ti = threatIntelService.check(normalizedUrl, domain);

        // Se ThreatIntel já trouxe hits/evidências, agregamos nas listas globais
        if (ti != null) {
            if (ti.getRuleHits() != null) {
                hits.addAll(ti.getRuleHits());
            }
            if (ti.getEvidence() != null) {
                evidence.addAll(ti.getEvidence());
            }
        }

        // Tenta decidir apenas com Threat Intel
        Result tiDecision = decideByThreatIntel(ti, rulesScoreBase, hits, evidence);
        if (tiDecision != null) {
            // Se ThreatIntel for conclusivo (MALICIOUS ou CLEAN em certos cenários),
            // retornamos diretamente sem chamar IA
            return tiDecision;
        }

        // Se ThreatIntel não foi conclusivo, construímos um resumo de evidências
        String mainEvidenceSummary = evidence.isEmpty()
                ? "Sem evidências fortes de Threat Intel."
                : String.join(" | ", evidence);

        // 2) IA externa (LLM) – chamada somente quando a Threat Intel não decidiu
        ExternalAiResponse aiResp = externalAiClient.classify(
                normalizedUrl,
                domain,
                rulesScoreBase,
                mainEvidenceSummary);

        // Decisão final baseada apenas na IA (mas respeitando o score base das regras)
        return decideByAi(aiResp, rulesScoreBase, hits, evidence);
    }

    /**
     * Decide com base apenas na Threat Intelligence.
     * Se a reputação for MALICIOUS ou claramente CLEAN (com poucas regras
     * disparadas),
     * já devolvemos um veredito final.
     *
     * @return Result se ThreatIntel foi conclusivo, ou null se for inconclusivo
     *         (UNKNOWN)
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

        // Caso 1: reputação claramente maliciosa
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

        // Caso 2: reputação limpa e regras locais não estão muito fortes
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

        // Caso 3: reputação UNKNOWN → não decide, apenas registra
        if (rep == Reputation.UNKNOWN) {
            hits.add("THREAT_INTEL_UNKNOWN");
        }

        // Deixa para IA decidir
        return null;
    }

    private Result decideByAi(
            ExternalAiResponse aiResp,
            int rulesScoreBase,
            List<String> hits,
            List<String> evidence) {
        // Falha na chamada da IA → mantemos UNKNOWN com score das regras
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

        // Score final é o máximo entre o score das regras e o score de risco da IA
        int finalScore = (int) Math.round(Math.max(riskScore * 100, rulesScoreBase));

        if (aiResp.getExplanation() != null && !aiResp.getExplanation().isBlank()) {
            evidence.add("IA: " + aiResp.getExplanation());
        }

        // Caso 1: IA tem alto risco e marca como phishing → SUSPECT
        if (phishing && riskScore >= IA_PHISHING_THRESHOLD) {
            hits.add("IA_PHISHING");
            return new Result(
                    Verdict.SUSPECT,
                    Math.max(finalScore, IA_MIN_SUSPECT_SCORE),
                    "IA",
                    hits,
                    evidence);
        }

        // Caso 2: IA vê risco baixo e não vê phishing → LEGIT
        if (!phishing && riskScore <= IA_CLEAN_THRESHOLD) {
            hits.add("IA_CLEAN");
            return new Result(
                    Verdict.LEGIT,
                    finalScore,
                    "IA",
                    hits,
                    evidence);
        }

        // Caso 3: IA não tem certeza → UNKNOWN
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
