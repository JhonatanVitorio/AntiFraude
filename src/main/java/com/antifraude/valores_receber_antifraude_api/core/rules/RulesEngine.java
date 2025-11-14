package com.antifraude.valores_receber_antifraude_api.core.rules;

import com.antifraude.valores_receber_antifraude_api.core.model.enums.Verdict;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class RulesEngine {

    public static class Result {
        public final Verdict verdict;
        public final int score;
        public final List<String> ruleHits;
        public final List<String> evidence;

        public Result(Verdict verdict, int score, List<String> ruleHits, List<String> evidence) {
            this.verdict = verdict;
            this.score = score;
            this.ruleHits = ruleHits;
            this.evidence = evidence;
        }
    }

    /**
     * Motor de regras local.
     *
     * Ideia:
     * - Somar "pontos de risco" conforme padrões.
     * - Se passar de um limiar alto → SUSPECT.
     * - Se não tiver sinal forte → UNKNOWN (deixa ThreatIntel + IA decidirem).
     * - Só marcamos LEGIT quando não há nenhuma regra de risco acionada.
     */
    public Result evaluate(String normalizedUrl, String domain) {
        String url = normalizedUrl == null ? "" : normalizedUrl.toLowerCase(Locale.ROOT);
        String host = domain == null ? "" : domain.toLowerCase(Locale.ROOT);

        int score = 0;
        List<String> hits = new ArrayList<>();
        List<String> evidence = new ArrayList<>();

        // --- Regras de risco baixo / moderado (não decidem sozinhas) ---

        // HTTP sem TLS
        if (url.startsWith("http://")) {
            score += 25;
            hits.add("HTTP_NO_TLS");
            evidence.add("URL usa http (sem TLS).");
        }

        // Comprimento de URL muito grande (tende a ser suspeito)
        if (url.length() > 150) {
            score += 30;
            hits.add("LONG_URL");
            evidence.add("URL muito longa (mais de 150 caracteres).");
        }

        // Parâmetros estranhos no path
        if (url.contains("@") || url.contains("..") || url.contains("%00")) {
            score += 30;
            hits.add("SUSPICIOUS_PATH");
            evidence.add("Caminho da URL contém padrões suspeitos (@, .., %00).");
        }

        // --- Regras de risco alto (podem empurrar para SUSPECT) ---

        // Palavras muito delicadas em contexto financeiro
        if (host.contains("secure")
                || host.contains("auth")
                || host.contains("banking")
                || host.contains("login")
                || host.contains("account")
                || host.contains("pix")
                || host.contains("boleto")) {

            score += 40;
            hits.add("SUSPICIOUS_KEYWORD");
            evidence.add("Domínio contém palavras sensíveis (secure, auth, banking, login, account, pix, boleto).");
        }

        // Encurtadores/“parecidos” falsos
        if (host.contains("bit-llly")
                || host.contains("tinyurl-security")
                || host.contains("secure-link")
                || host.contains("short-secure")) {

            score += 40;
            hits.add("FAKE_SHORTENER");
            evidence.add("Domínio parece encurtador/seguro falso (bit-llly, tinyurl-security, etc.).");
        }

        // --- Decisão final baseada no score ---

        Verdict verdict;
        int finalScore = Math.min(score, 100);

        // Limiares (ajuste fino do comportamento)
        final int SUSPECT_THRESHOLD = 60;
        final int LEGIT_MAX_SCORE_FOR_RULES = 0; // se tiver qualquer ponto, deixamos UNKNOWN

        if (finalScore >= SUSPECT_THRESHOLD) {
            verdict = Verdict.SUSPECT;
        } else if (finalScore <= LEGIT_MAX_SCORE_FOR_RULES && hits.isEmpty()) {
            // Somente consideramos LEGIT quando NENHUMA regra disparou (score 0 e sem
            // hits).
            verdict = Verdict.LEGIT;
        } else {
            // Qualquer caso intermediário fica como UNKNOWN
            // para permitir que ThreatIntel + IA decidam.
            verdict = Verdict.UNKNOWN;
        }

        return new Result(verdict, finalScore, hits, evidence);
    }
}
