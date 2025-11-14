package com.antifraude.valores_receber_antifraude_api.aiAgent;

import org.springframework.stereotype.Component;

/**
 * Stub de cliente de IA externa .
 *
 * Nesta versão, não há chamada real para OpenAI / modelos externos.
 * Em vez disso, usamos uma heurística simples baseada em palavras-chave
 * apenas para simular o comportamento de uma IA que classifica URLs.
 */
@Component
public class ExternalAiClient {

    /**
     * Classifica a URL com base em heurísticas de palavras-chave.
     *
     * @param normalizedUrl   URL normalizada
     * @param domain          domínio extraído
     * @param rulesScoreBase  score vindo das regras locais (não usado aqui, mas já
     *                        está no método para futura integração)
     * @param evidenceSummary resumo de evidências vindo da Threat Intel
     *
     * @return {@link ExternalAiResponse} com riskScore, flag de phishing e
     *         explicação
     */
    public ExternalAiResponse classify(
            String normalizedUrl,
            String domain,
            int rulesScoreBase,
            String evidenceSummary) {

        // Concatenamos URL + domínio e jogamos para minúsculo para facilitar os
        // contains(...)
        String all = (normalizedUrl + " " + domain).toLowerCase();

        ExternalAiResponse resp = new ExternalAiResponse();

        // CASOS BEM SUSPEITOS
        // Engloba vários padrões de golpes que mapeamos previamente
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

            resp.setRiskScore(0.9); // risco bem alto
            resp.setPhishing(true);
            resp.setExplanation(
                    "Heurística de demo da IA: URL contém padrões típicos de golpe "
                            + "(valores a receber, FGTS, Caixa, Receita, WhatsApp falso, IRPF, PIX, encurtadores, banking secure).");
            return resp;
        }

        // CASOS MUITO LIMPOS (domínios oficiais bem conhecidos)
        if (all.contains("caixa.gov.br")
                || all.contains("bb.com.br")
                || all.endsWith("gov.br")
                || all.contains("meu.inss.gov.br")
                || all.contains("google.com")
                || all.contains("magazineluiza.com.br")) {

            resp.setRiskScore(0.1); // risco bem baixo
            resp.setPhishing(false);
            resp.setExplanation("Heurística de demo da IA: domínio oficial reconhecido como confiável.");
            return resp;
        }

        // CASOS MEIO TERMO (IA não viu nada muito suspeito nem 100% limpo)
        resp.setRiskScore(0.5);
        resp.setPhishing(false);
        resp.setExplanation("Heurística de demo da IA: não achou nada muito suspeito nem claramente confiável.");
        return resp;
    }
}
