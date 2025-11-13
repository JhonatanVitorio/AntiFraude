package com.antifraude.valores_receber_antifraude_api.application.service.rules;

import com.antifraude.valores_receber_antifraude_api.domain.model.enums.Verdict;
import com.antifraude.valores_receber_antifraude_api.shared.util.DomainUtils;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class RulesEngine {

    public static class Result {
        public final int score; // 0–100
        public final Verdict verdict;
        public final List<String> ruleHits; // códigos das regras acionadas
        public final List<String> evidence; // explicações curtas

        public Result(int score, Verdict verdict, List<String> ruleHits, List<String> evidence) {
            this.score = score;
            this.verdict = verdict;
            this.ruleHits = ruleHits;
            this.evidence = evidence;
        }
    }

    // ===== Pesos (ajustáveis) =====
    private static final int W_HTTP_NO_TLS = 25;
    private static final int W_PHISHING_KEYWORDS = 20; // pode acumular até +40
    private static final int W_NON_GOV_DOMAIN = 30;
    private static final int W_BRAND_MISLEAD = 25;
    private static final int W_URL_SHORTENER = 20;
    private static final int W_SUSPICIOUS_TLD = 15;
    private static final int W_EXCESS_SUBDOMAINS = 10;
    private static final int W_DIGIT_HEAVY_PATH = 10;
    private static final int W_QUERY_SENSITIVE_KEYS = 25;

    private static final int MAX_ACCUM_PHISHING = 40;

    // Palavras-chave fortes do golpe “valores a receber”
    private static final Pattern KW_PHISHING = Pattern.compile(
            "(valores|receber|resgate|liberar|consulta|pix|saldo|gov|login|senha|cpf)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern QUERY_SENSITIVE = Pattern.compile(
            "(cpf|senha|token|codigo|code|chave|key)=",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATH_DIGITHEAVY = Pattern.compile(".*/?[a-zA-Z]*\\d{8,}.*");

    public Result evaluate(String normalizedUrl, String host) {
        int score = 0;
        List<String> hits = new ArrayList<>();
        List<String> ev = new ArrayList<>();

        String url = normalizedUrl != null ? normalizedUrl : "";
        String h = host != null ? host : "";

        // HTTP sem TLS
        if (url.toLowerCase().startsWith("http://")) {
            score += W_HTTP_NO_TLS;
            hits.add("HTTP_NO_TLS");
            ev.add("URL usa http (sem TLS)");
        }

        // Encurtador
        if (DomainUtils.isUrlShortener(h)) {
            score += W_URL_SHORTENER;
            hits.add("URL_SHORTENER");
            ev.add("Domínio encurtador: " + h);
        }

        // TLD suspeito
        if (DomainUtils.hasSuspiciousTld(h)) {
            score += W_SUSPICIOUS_TLD;
            hits.add("SUSPICIOUS_TLD");
            ev.add("TLD potencialmente arriscado");
        }

        // Muitos subdomínios
        if (DomainUtils.countLabels(h) >= 4) {
            score += W_EXCESS_SUBDOMAINS;
            hits.add("EXCESS_SUBDOMAINS");
            ev.add("Muitos níveis de subdomínio");
        }

        // Palavras de phishing (no path ou host)
        int phishAdds = 0;
        if (KW_PHISHING.matcher(url).find()) {
            phishAdds += W_PHISHING_KEYWORDS;
        }
        if (KW_PHISHING.matcher(h).find()) {
            phishAdds += W_PHISHING_KEYWORDS;
        }
        if (phishAdds > 0) {
            int add = Math.min(phishAdds, MAX_ACCUM_PHISHING);
            score += add;
            hits.add("PHISHING_KEYWORDS");
            ev.add("Palavras-chave suspeitas no URL/host");
        }

        // Se menciona gov/valores e NÃO é gov.br → penaliza
        boolean mentionsGovOrValores = url.toLowerCase().contains("gov") || url.toLowerCase().contains("valores");
        if (mentionsGovOrValores && !DomainUtils.isGovBr(h)) {
            score += W_NON_GOV_DOMAIN;
            hits.add("NON_GOV_DOMAIN");
            ev.add("Tema governamental sem domínio .gov.br");
        }

        // “gov” no subdomínio mas não gov.br
        if (DomainUtils.brandInSubdomainButNotOfficialGov(h)) {
            score += W_BRAND_MISLEAD;
            hits.add("BRAND_MISLEAD");
            ev.add("‘gov’ em subdomínio mas domínio base não é gov.br");
        }

        // Path com muitos dígitos (IDs longos usados em iscas)
        if (PATH_DIGITHEAVY.matcher(url).matches()) {
            score += W_DIGIT_HEAVY_PATH;
            hits.add("DIGIT_HEAVY_PATH");
            ev.add("Caminho com sequência longa de dígitos");
        }

        // Query pedindo dados sensíveis
        if (QUERY_SENSITIVE.matcher(url).find()) {
            score += W_QUERY_SENSITIVE_KEYS;
            hits.add("QUERY_SENSITIVE_KEYS");
            ev.add("Parâmetros sensíveis na URL (ex.: cpf, senha, token)");
        }

        // clamp
        score = Math.max(0, Math.min(score, 100));
        Verdict verdict;
        if (score >= 70)
            verdict = Verdict.SUSPECT;
        else if (score <= 30)
            verdict = Verdict.LEGIT;
        else
            verdict = Verdict.UNKNOWN;

        return new Result(score, verdict, Collections.unmodifiableList(hits), Collections.unmodifiableList(ev));
    }
}
