package com.antifraude.valores_receber_antifraude_api.application.service.threat;

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
    public ThreatIntelResult check(String normalizedUrl, String domain) {
        ThreatIntelResult result = new ThreatIntelResult();

        String url = normalizedUrl.toLowerCase(Locale.ROOT);
        String host = domain.toLowerCase(Locale.ROOT);

        // 1) Tenta usar VirusTotal primeiro
        VirusTotalClient.VirusTotalResult vt = virusTotalClient.checkUrl(url);
        if (vt.reputation != ThreatReputation.UNKNOWN) {
            result.setReputation(vt.reputation);
            result.addHit("VT_" + vt.reputation.name());
            result.addEvidence("VirusTotal: " + vt.evidence
                    + " (malicious=" + vt.malicious
                    + ", suspicious=" + vt.suspicious
                    + ", harmless=" + vt.harmless + ")");
            return result;
        }

        // 2) Se o VirusTotal não der nada forte, caímos nas heurísticas locais
        return fallbackLocalHeuristics(url, host);
    }

    private ThreatIntelResult fallbackLocalHeuristics(String url, String host) {
        ThreatIntelResult result = new ThreatIntelResult();

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
                    "Domínio parecido com WhatsApp, mas escrito incorretamente (possível golpe).");
        }

        if (isTrustedDomain(host)) {
            result.setReputation(ThreatReputation.CLEAN);
            result.addHit("THREAT_INTEL_LOCAL_CLEAN");
            result.addEvidence("Heurística local: domínio considerado confiável.");
            return result;
        }

        result.setReputation(ThreatReputation.UNKNOWN);
        result.addHit("THREAT_INTEL_UNKNOWN");
        result.addEvidence("Nem VirusTotal nem heurísticas locais deram um sinal forte.");
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
                || host.equals("www.example.org");
    }

    private ThreatIntelResult malicious(ThreatIntelResult result, String hit, String evidence) {
        result.setReputation(ThreatReputation.MALICIOUS);
        result.addHit(hit);
        result.addEvidence(evidence);
        return result;
    }
}
