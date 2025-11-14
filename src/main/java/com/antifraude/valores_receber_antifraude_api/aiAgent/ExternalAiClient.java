package com.antifraude.valores_receber_antifraude_api.aiAgent;

import org.springframework.stereotype.Component;

/**
 * Stub de IA externa (LLM).
 * Aqui você pode futuramente integrar com OpenAI, etc.
 */
@Component
public class ExternalAiClient {

    public ExternalAiResponse classify(
            String normalizedUrl,
            String domain,
            int rulesScoreBase,
            String evidenceSummary) {
        String all = (normalizedUrl + " " + domain).toLowerCase();

        ExternalAiResponse resp = new ExternalAiResponse();

        // 🟥 CASOS BEM SUSPEITOS (incluindo os que você relatou)
        if (all.contains("valoresareceber")
                || all.contains("valores-a-receber")
                || all.contains("fgts")
                || all.contains("caixa-gov-br.online")
                || all.contains("receitafederal-gov.online")
                || all.contains("whatsap-confirmacao")
                || all.contains("whatsap-verificador")
                || all.contains("simulador-irpf.site")
                || all.contains("irpf")
                || all.contains("secure-pay-pix")
                || all.contains("bit-llly-secure")
                || all.contains("tinyurl-security-check")
                || all.contains("banking-secure-auth")
                || all.contains("secure-auth")) {

            resp.setRiskScore(0.9); // bem alto
            resp.setPhishing(true);
            resp.setExplanation(
                    "Heurística de demo da IA: URL contém padrões típicos de golpe (valores a receber, FGTS, Caixa, Receita, WhatsApp falso, IRPF, PIX, encurtadores, banking secure).");
            return resp;
        }

        // 🟢 CASOS MUITO LIMPOS (domínios oficiais)
        if (all.contains("caixa.gov.br")
                || all.contains("bb.com.br")
                || all.endsWith("gov.br")
                || all.contains("meu.inss.gov.br")
                || all.contains("google.com")
                || all.contains("magazineluiza.com.br")) {

            resp.setRiskScore(0.1); // bem baixo
            resp.setPhishing(false);
            resp.setExplanation("Heurística de demo da IA: domínio oficial reconhecido como confiável.");
            return resp;
        }

        // 🟡 CASOS MEIO TERMO (INCONCLUSIVOS)
        resp.setRiskScore(0.5);
        resp.setPhishing(false);
        resp.setExplanation("Heurística de demo da IA: não achou nada muito suspeito nem claramente confiável.");
        return resp;
    }
}
