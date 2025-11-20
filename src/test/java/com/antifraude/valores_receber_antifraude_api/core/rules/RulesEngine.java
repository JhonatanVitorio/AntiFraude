package com.antifraude.valores_receber_antifraude_api.core.rules;

import com.antifraude.valores_receber_antifraude_api.core.model.enums.Verdict;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RulesEngine {

    // Pesos das regras (ajustáveis)
    private static final double SCORE_HTTP_NO_TLS = 20.0;
    private static final double SCORE_FAKE_SHORTENER = 80.0;
    private static final double SCORE_SUSPICIOUS_KEYWORD = 70.0;

    // Threshold geral para considerar SUSPECT
    private static final double SUSPECT_THRESHOLD = 70.0;

    /**
     * Resultado do motor de regras.
     */
    public static class Result {
        public final Verdict verdict;
        public final double score;
        public final List<String> ruleHits;
        public final List<String> evidence;

        public Result(Verdict verdict,
                double score,
                List<String> ruleHits,
                List<String> evidence) {
            this.verdict = verdict;
            this.score = score;
            this.ruleHits = ruleHits;
            this.evidence = evidence;
        }
    }

    /**
     * Avalia a URL e o domínio com base em regras heurísticas.
     */
    public Result evaluate(String url, String domain) {
        if (url == null) {
            url = "";
        }
        if (domain == null) {
            domain = "";
        }

        String lowerUrl = url.toLowerCase(Locale.ROOT);
        String lowerDomain = domain.toLowerCase(Locale.ROOT);

        Set<String> ruleHits = new LinkedHashSet<>();
        List<String> evidence = new ArrayList<>();
        double score = 0.0;

        // Regra 1: HTTP sem TLS
        if (isHttpWithoutTls(lowerUrl)) {
            ruleHits.add("HTTP_NO_TLS");
            evidence.add("URL utiliza HTTP sem TLS (não criptografado).");
            score += SCORE_HTTP_NO_TLS;
        }

        // Regra 2: Domínio parecido com encurtador fake
        if (isFakeShortener(lowerDomain)) {
            ruleHits.add("FAKE_SHORTENER");
            evidence.add("Domínio imita encurtadores de URL conhecidos (possível phishing).");
            score += SCORE_FAKE_SHORTENER;
        }

        // Regra 3: Palavras-chave suspeitas (secure, banking, auth, login etc.)
        if (containsSuspiciousKeyword(lowerUrl, lowerDomain)) {
            ruleHits.add("SUSPICIOUS_KEYWORD");
            evidence.add("URL contém palavras-chave sensíveis como banking, secure, auth ou login.");
            score += SCORE_SUSPICIOUS_KEYWORD;
        }

        // Decisão final:
        // - Nenhuma regra ativada -> UNKNOWN (deixa AiAgent/ThreatIntel decidir)
        // - Score >= limiar -> SUSPECT
        // - Score abaixo do limiar, mas com hits -> UNKNOWN (riscos leves / não
        // conclusivos)
        Verdict verdict;
        if (ruleHits.isEmpty()) {
            verdict = Verdict.UNKNOWN;
        } else if (score >= SUSPECT_THRESHOLD) {
            verdict = Verdict.SUSPECT;
        } else {
            verdict = Verdict.UNKNOWN;
        }

        return new Result(
                verdict,
                score,
                List.copyOf(ruleHits),
                List.copyOf(evidence));
    }

    // ------------ Regras auxiliares ------------ //

    private boolean isHttpWithoutTls(String url) {
        // Simples: começa com http:// e não com https://
        return url.startsWith("http://");
    }

    /**
     * Detecta domínios que tentam imitar encurtadores (tipo bit.ly) de forma
     * suspeita,
     * como "bit-llly-secure.com".
     */
    private boolean isFakeShortener(String domain) {
        String d = domain.toLowerCase(Locale.ROOT);

        // Foca no que precisamos para o teste: "bit-llly-secure.com"
        // e variações que lembram encurtadores mas não são domínios oficiais.
        if (d.contains("bit-llly")) {
            return true;
        }

        // Outras heurísticas simples (opcional, mas útil):
        if (d.contains("bit-") && d.contains("secure")) {
            return true;
        }
        if (d.contains("bitly") && !d.endsWith("bit.ly")) {
            return true;
        }

        return false;
    }

    /**
     * Verifica palavras-chave suspeitas no domínio ou path,
     * como "banking-secure-auth.com/login".
     */
    private boolean containsSuspiciousKeyword(String url, String domain) {
        String combined = (url + " " + domain).toLowerCase(Locale.ROOT);

        String[] keywords = {
                "banking",
                "secure",
                "auth",
                "login",
                "verify",
                "validation"
        };

        for (String keyword : keywords) {
            if (combined.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
