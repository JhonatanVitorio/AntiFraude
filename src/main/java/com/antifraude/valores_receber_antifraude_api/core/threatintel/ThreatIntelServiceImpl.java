package com.antifraude.valores_receber_antifraude_api.core.threatintel;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class ThreatIntelServiceImpl implements ThreatIntelService {

    private static final String CAIXA_DOMAIN = "caixa.gov.br";
    private static final String RECEITA_DOMAIN = "receita.economia.gov.br";

    private final VirusTotalClient virusTotalClient;

    public ThreatIntelServiceImpl(VirusTotalClient virusTotalClient) {
        this.virusTotalClient = virusTotalClient;
    }

    @Override
    public Result check(String normalizedUrl, String domain) {
        Result result = new Result();

        String url = normalizedUrl.toLowerCase(Locale.ROOT);
        String host = domain.toLowerCase(Locale.ROOT);

        // 1) Tenta usar VirusTotal primeiro (stub)
        VirusTotalClient.VirusTotalResult vt = virusTotalClient.checkUrl(url);
        if (vt.reputation != Reputation.UNKNOWN) {
            result.setReputation(vt.reputation);
            result.addHit("VT_" + vt.reputation.name());
            result.addEvidence("VirusTotal (stub): " + vt.evidence
                    + " (malicious=" + vt.malicious
                    + ", suspicious=" + vt.suspicious
                    + ", harmless=" + vt.harmless + ")");
            return result;
        }

        // 2) Se o VirusTotal não der nada forte, cai nas heurísticas locais
        return fallbackLocalHeuristics(url, host, result);
    }

    private Result fallbackLocalHeuristics(String url, String host, Result result) {
        // Domínios falsos parecidos com Caixa / Receita / WhatsApp
        if (isFakeCaixa(host)) {
            return malicious(result,
                    "THREAT_INTEL_TYPO_CAIXA",
                    "Domínio parecido com Caixa, mas não é o oficial (possível golpe).");
        }

        if (isFakeReceita(host)) {
            return malicious(result,
                    "THREAT_INTEL_TYPO_RECEITA",
                    "Domínio parecido com Receita Federal, mas não é o oficial (possível golpe).");
        }

        if (isFakeWhatsapp(host)) {
            return malicious(result,
                    "THREAT_INTEL_TYPO_WHATSAPP",
                    "Domínio parecido com WhatsApp escrito incorretamente (possível golpe).");
        }

        // ⚠️ Padrões bem suspeitos relacionados a banco/IRPF/segurança
        if (host.contains("simulador-irpf.site")
                || host.contains("irpf")
                || host.contains("banking-secure-auth")
                || host.contains("secure-auth")
                || host.contains("bit-llly-secure")
                || host.contains("tinyurl-security")) {
            return malicious(result,
                    "THREAT_INTEL_SUSPICIOUS_PATTERN",
                    "Padrões típicos de golpe: IRPF, secure-auth, encurtadores falsos, etc.");
        }

        // Domínios confiáveis conhecidos
        if (isTrustedDomain(host)) {
            result.setReputation(Reputation.CLEAN);
            result.addHit("THREAT_INTEL_LOCAL_CLEAN");
            result.addEvidence("Heurística local: domínio considerado confiável.");
            return result;
        }

        // Nenhum sinal forte → UNKNOWN
        result.setReputation(Reputation.UNKNOWN);
        result.addHit("THREAT_INTEL_UNKNOWN");
        result.addEvidence("Nem VirusTotal (stub) nem heurísticas locais deram um sinal forte.");
        return result;
    }

    private boolean isFakeCaixa(String host) {
        return (host.contains("caix") || host.contains("caixa"))
                && !host.endsWith(CAIXA_DOMAIN);
    }

    private boolean isFakeReceita(String host) {
        return host.contains("receita")
                && !host.endsWith(RECEITA_DOMAIN);
    }

    private boolean isFakeWhatsapp(String host) {
        // pega "whatsap", "whatsap-" etc, mas não whatsapp.com/.net
        return host.contains("whatsap")
                && !host.contains("whatsapp.com")
                && !host.contains("whatsapp.net");
    }

    private boolean isTrustedDomain(String host) {
        return host.endsWith("bb.com.br")
                || host.endsWith(CAIXA_DOMAIN)
                || host.endsWith("gov.br")
                || host.endsWith(RECEITA_DOMAIN)
                || host.endsWith("meu.inss.gov.br")
                || host.equals("www.example.org")
                || host.endsWith("google.com")
                || host.endsWith("magazineluiza.com.br");
    }

    private Result malicious(Result result, String hit, String evidence) {
        result.setReputation(Reputation.MALICIOUS);
        result.addHit(hit);
        result.addEvidence(evidence);
        return result;
    }

    /**
     * Client do VirusTotal fica como classe interna da implementação.
     * Aqui está com um stub inteligente pra teste.
     */
    @Component
    public static class VirusTotalClient {

        public static class VirusTotalResult {
            public final Reputation reputation;
            public final int malicious;
            public final int suspicious;
            public final int harmless;
            public final String evidence;

            public VirusTotalResult(
                    Reputation reputation,
                    int malicious,
                    int suspicious,
                    int harmless,
                    String evidence) {
                this.reputation = reputation;
                this.malicious = malicious;
                this.suspicious = suspicious;
                this.harmless = harmless;
                this.evidence = evidence;
            }

            public static VirusTotalResult unknown() {
                return new VirusTotalResult(
                        Reputation.UNKNOWN,
                        0,
                        0,
                        0,
                        "Sem dados do VirusTotal (stub).");
            }
        }

        public VirusTotalResult checkUrl(String normalizedUrl) {
            String url = normalizedUrl.toLowerCase();

            // 🟥 Padrões fortes de golpe (incluindo os que você citou)
            if (url.contains("valoresareceber")
                    || url.contains("valores-a-receber")
                    || url.contains("fgts")
                    || url.contains("caixa-gov-br.online")
                    || url.contains("receitafederal-gov.online")
                    || url.contains("whatsap-confirmacao")
                    || url.contains("whatsap-verificador")
                    || url.contains("simulador-irpf.site")
                    || url.contains("secure-pay-pix")
                    || url.contains("bit-llly-secure")
                    || url.contains("tinyurl-security-check")
                    || url.contains("banking-secure-auth")) {

                return new VirusTotalResult(
                        Reputation.MALICIOUS,
                        10,
                        5,
                        0,
                        "Stub VT: URL bate com padrões típicos de golpe (valores a receber / FGTS / Caixa / Receita / WhatsApp / IRPF / PIX / encurtador / banking secure).");
            }

            // demais casos: UNKNOWN
            return VirusTotalResult.unknown();
        }
    }
}
