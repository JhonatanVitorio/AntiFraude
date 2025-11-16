package com.antifraude.valores_receber_antifraude_api.aiAgent;

import com.antifraude.valores_receber_antifraude_api.core.model.enums.Verdict;
import com.antifraude.valores_receber_antifraude_api.core.threatintel.ThreatIntelService;
import com.antifraude.valores_receber_antifraude_api.core.threatintel.ThreatIntelService.Reputation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Serviço responsável por orquestrar a decisão final de risco da URL,
 * combinando ThreatIntel + IA externa (OpenAI).
 */
@Service
public class AiAgentService {

    // Limiar de decisão da IA - mais agressivo para o projeto
    // Acima de 0.40 já consideramos SUSPECT (melhor errar para o lado da segurança)
    // Abaixo de 0.20 consideramos LEGIT
    private static final double RISK_SUSPECT_THRESHOLD = 0.40;
    private static final double RISK_LEGIT_THRESHOLD = 0.20;

    private final ThreatIntelService threatIntelService;
    private final ExternalAiClient externalAiClient;

    public AiAgentService(
            ThreatIntelService threatIntelService,
            ExternalAiClient externalAiClient) {
        this.threatIntelService = threatIntelService;
        this.externalAiClient = externalAiClient;
    }

    /**
     * Objeto de retorno do pipeline de IA (usado pelo CheckService).
     */
    public static class Result {
        public final Verdict verdict; // SUSPECT / LEGIT / UNKNOWN
        public final int score; // 0 a 100
        public final String source; // "IA" ou "THREAT_INTEL"
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
     * Pipeline principal:
     * 1) ThreatIntel (VirusTotal stub + heurísticas)
     * 2) Se ThreatIntel não decidir, chama IA externa
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

        // Tenta decidir só com ThreatIntel (malicious muito claro ou clean muito claro)
        Result tiDecision = decideByThreatIntel(ti, rulesScoreBase, hits, evidence);
        if (tiDecision != null) {
            return tiDecision;
        }

        // 2) ThreatIntel foi inconclusivo → chama IA externa
        String evidenceSummary = evidence.isEmpty()
                ? "Sem evidências fortes de Threat Intel."
                : String.join(" | ", evidence);

        ExternalAiResponse aiResp = externalAiClient.classify(
                normalizedUrl,
                domain,
                rulesScoreBase,
                evidenceSummary);

        return decideByAi(aiResp, rulesScoreBase, hits, evidence);
    }

    /**
     * Decide apenas com ThreatIntel quando possível.
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

        // Caso 1: reputação claramente maliciosa → SUSPECT direto
        if (rep == Reputation.MALICIOUS) {
            int score = Math.max(rulesScoreBase, 85);
            hits.add("THREAT_INTEL_MALICIOUS");
            return new Result(
                    Verdict.SUSPECT,
                    score,
                    "THREAT_INTEL",
                    hits,
                    evidence);
        }

        // Caso 2: reputação limpa e score de regras muito baixo → LEGIT
        if (rep == Reputation.CLEAN && rulesScoreBase <= 10) {
            int score = Math.min(rulesScoreBase, 15);
            hits.add("THREAT_INTEL_CLEAN");
            return new Result(
                    Verdict.LEGIT,
                    score,
                    "THREAT_INTEL",
                    hits,
                    evidence);
        }

        // Caso 3: UNKNOWN → apenas loga e deixa a IA decidir
        if (rep == Reputation.UNKNOWN) {
            hits.add("THREAT_INTEL_UNKNOWN");
        }

        return null; // deixa para IA
    }

    /**
     * Decide com base na resposta da IA externa.
     */
    private Result decideByAi(
            ExternalAiResponse aiResp,
            int rulesScoreBase,
            List<String> hits,
            List<String> evidence) {

        if (aiResp == null) {
            hits.add("IA_ERROR");
            evidence.add("IA externa indisponível; mantendo UNKNOWN.");
            return new Result(
                    Verdict.UNKNOWN,
                    rulesScoreBase,
                    "IA",
                    hits,
                    evidence);
        }

        Double risk = aiResp.getRiskScore();
        Boolean phishing = aiResp.getPhishing();
        String explanation = aiResp.getExplanation();

        double riskScore = (risk != null) ? risk : 0.5;
        boolean isPhishing = (phishing != null) && phishing;

        int aiScore = (int) Math.round(riskScore * 100.0);
        int finalScore = Math.max(rulesScoreBase, aiScore);

        if (explanation != null && !explanation.isBlank()) {
            evidence.add("IA: " + explanation);
        }

        // Caso SUSPECT: risco alto ou IA marcando phishing
        if (isPhishing || riskScore >= RISK_SUSPECT_THRESHOLD) {
            hits.add("IA_PHISHING");
            return new Result(
                    Verdict.SUSPECT,
                    Math.max(finalScore, 80),
                    "IA",
                    hits,
                    evidence);
        }

        // Caso LEGIT: risco bem baixo e sem phishing
        if (!isPhishing && riskScore <= RISK_LEGIT_THRESHOLD) {
            hits.add("IA_CLEAN");
            return new Result(
                    Verdict.LEGIT,
                    Math.min(finalScore, 20),
                    "IA",
                    hits,
                    evidence);
        }

        // Caso intermediário: IA não teve confiança suficiente → UNKNOWN
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
