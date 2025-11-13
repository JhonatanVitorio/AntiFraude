package com.antifraude.valores_receber_antifraude_api.application.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente para um modelo LLM (OpenAI) usado como classificador de phishing.
 *
 * Usa o endpoint /v1/chat/completions:
 * - model
 * - messages (system + user)
 *
 * A resposta da IA deve ser um JSON do tipo:
 * { "riskScore": 0.85, "phishing": true, "explanation": "..." }
 */
@Component
public class ExternalAiClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalAiClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public ExternalAiClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${antifraude.external-ai.base-url}") String baseUrl,
            @Value("${antifraude.external-ai.api-key}") String apiKey,
            @Value("${antifraude.external-ai.model}") String model) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    public ExternalAiResponse classify(String url, String domain, int localScore, String mainEvidenceSummary) {
        try {
            String endpoint = baseUrl + "/chat/completions";

            String prompt = buildPrompt(url, domain, localScore, mainEvidenceSummary);

            // Corpo esperado pelo Chat Completions
            String jsonBody = """
                    {
                      "model": "%s",
                      "response_format": { "type": "json_object" },
                      "messages": [
                        {
                          "role": "system",
                          "content": "Você é uma IA especialista em detecção de phishing bancário. Sempre responda em JSON com os campos: riskScore (0 a 1), phishing (true/false), explanation (texto curto). Não retorne nenhum texto fora do JSON."
                        },
                        {
                          "role": "user",
                          "content": %s
                        }
                      ]
                    }
                    """
                    .formatted(model, objectMapper.writeValueAsString(prompt));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Falha na chamada da IA externa: status={}", response.getStatusCode());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                log.warn("Resposta inesperada da IA externa (sem choices)");
                return null;
            }

            JsonNode message = choices.get(0).path("message");
            String content = message.path("content").asText();

            if (content == null || content.isBlank()) {
                log.warn("Conteúdo vazio na resposta da IA externa");
                return null;
            }

            // content deve ser um JSON string com riskScore, phishing, explanation
            JsonNode json = objectMapper.readTree(content);

            ExternalAiResponse result = new ExternalAiResponse();
            if (json.has("riskScore")) {
                result.setRiskScore(json.get("riskScore").asDouble(0.0));
            }
            if (json.has("phishing")) {
                result.setPhishing(json.get("phishing").asBoolean(false));
            }
            if (json.has("explanation")) {
                result.setExplanation(json.get("explanation").asText());
            }

            return result;

        } catch (Exception e) {
            log.warn("Erro ao chamar IA externa: {}", e.getMessage());
            return null;
        }
    }

    private String buildPrompt(String url, String domain, int localScore, String mainEvidence) {
        if (mainEvidence == null) {
            mainEvidence = "";
        }

        return """
                Analise o risco de phishing da seguinte URL.

                URL: %s
                Domínio: %s
                Score local do nosso motor: %d
                Evidência principal: %s

                Considere:
                - Se simula sites de bancos ou governo
                - Se pede credenciais, dados sensíveis, PIX, CPF, prêmios
                - Se o domínio parece fraudulento ou typosquatting
                - Se o contexto é típico de “valores a receber”, “prêmio”, “regularização de cadastro”, etc.

                Responda APENAS com um JSON no seguinte formato:
                {
                  "riskScore": 0.0-1.0,
                  "phishing": true/false,
                  "explanation": "texto curto explicando o motivo"
                }
                """.formatted(url, domain, localScore, mainEvidence);
    }
}
