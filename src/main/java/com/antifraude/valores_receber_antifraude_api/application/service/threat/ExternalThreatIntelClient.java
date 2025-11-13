package com.antifraude.valores_receber_antifraude_api.application.service.threat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente genérico para uma API externa de Threat Intelligence.
 * 
 * Aqui você configura:
 * - URL base da API
 * - Endpoint para checar URL
 * - Header de API-Key (se necessário)
 *
 * Depois é só adaptar o JSON conforme a API real que for usar.
 */
@Component
public class ExternalThreatIntelClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalThreatIntelClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public ExternalThreatIntelClient(
            RestTemplate restTemplate,
            @Value("${antifraude.threatintel.base-url:https://api-threat-intel.exemplo.com}") String baseUrl,
            @Value("${antifraude.threatintel.api-key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    /**
     * Faz uma chamada HTTP para a API externa de Threat Intel.
     *
     * Adapte o endpoint ("/v1/check-url") e a forma de envio (query param ou body)
     * conforme a documentação da API que for usar.
     */
    public ExternalThreatIntelResponse checkUrl(String url, String domain) {
        try {
            String endpoint = baseUrl + "/v1/check-url";

            // Exemplo: enviando JSON no corpo
            String jsonBody = """
                    {
                      "url": "%s",
                      "domain": "%s"
                    }
                    """.formatted(url, domain);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Se a API usar header de API Key
            if (apiKey != null && !apiKey.isBlank()) {
                headers.set("X-API-KEY", apiKey);
            }

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<ExternalThreatIntelResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    ExternalThreatIntelResponse.class);

            return response.getBody();
        } catch (Exception e) {
            log.warn("Falha ao consultar Threat Intel externa: {}", e.getMessage());
            return null;
        }
    }
}
