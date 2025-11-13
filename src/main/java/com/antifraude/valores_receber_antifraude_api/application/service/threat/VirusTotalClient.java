package com.antifraude.valores_receber_antifraude_api.application.service.threat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class VirusTotalClient {

    private static final Logger log = LoggerFactory.getLogger(VirusTotalClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;

    public VirusTotalClient(RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${antifraude.virustotal.api-key}") String apiKey,
            @Value("${antifraude.virustotal.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    /**
     * Consulta reputação de uma URL no VirusTotal.
     *
     * Fluxo:
     * - Gera o URL ID em base64 (sem "=")
     * - Faz GET /urls/{id}
     * - Analisa last_analysis_stats
     */
    public VirusTotalResult checkUrl(String url) {
        try {
            String urlId = encodeUrlId(url);
            String endpoint = baseUrl + "/urls/" + urlId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-apikey", apiKey);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("VirusTotal retornou status {} para URL {}", response.getStatusCode(), url);
                return VirusTotalResult.unknown("HTTP status não sucessful");
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode stats = root
                    .path("data")
                    .path("attributes")
                    .path("last_analysis_stats");

            if (stats.isMissingNode() || !stats.isObject()) {
                return VirusTotalResult.unknown("Sem last_analysis_stats no resultado");
            }

            int malicious = stats.path("malicious").asInt(0);
            int suspicious = stats.path("suspicious").asInt(0);
            int harmless = stats.path("harmless").asInt(0);

            // Regras simples de decisão com base nas estatísticas
            if (malicious > 0 || suspicious > 0) {
                return VirusTotalResult.malicious(
                        malicious, suspicious, harmless,
                        "VirusTotal identificou a URL como maliciosa/suspeita.");
            }

            if (harmless > 0 && malicious == 0 && suspicious == 0) {
                return VirusTotalResult.clean(
                        malicious, suspicious, harmless,
                        "VirusTotal classificou a URL como limpa.");
            }

            return VirusTotalResult.unknown(
                    malicious, suspicious, harmless,
                    "VirusTotal não possui sinal forte para esta URL.");

        } catch (Exception e) {
            log.warn("Erro ao consultar VirusTotal para URL {}: {}", url, e.getMessage());
            return VirusTotalResult.unknown("Erro ao chamar VirusTotal: " + e.getMessage());
        }
    }

    /**
     * Gera o identificador de URL como base64 URL-safe sem padding,
     * conforme a documentação do VirusTotal. :contentReference[oaicite:4]{index=4}
     */
    private String encodeUrlId(String url) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(url.getBytes(StandardCharsets.UTF_8));
    }

    public static class VirusTotalResult {
        public final ThreatReputation reputation;
        public final int malicious;
        public final int suspicious;
        public final int harmless;
        public final String evidence;

        private VirusTotalResult(ThreatReputation reputation, int malicious, int suspicious, int harmless,
                String evidence) {
            this.reputation = reputation;
            this.malicious = malicious;
            this.suspicious = suspicious;
            this.harmless = harmless;
            this.evidence = evidence;
        }

        public static VirusTotalResult malicious(int mal, int susp, int harml, String evidence) {
            return new VirusTotalResult(ThreatReputation.MALICIOUS, mal, susp, harml, evidence);
        }

        public static VirusTotalResult clean(int mal, int susp, int harml, String evidence) {
            return new VirusTotalResult(ThreatReputation.CLEAN, mal, susp, harml, evidence);
        }

        public static VirusTotalResult unknown(int mal, int susp, int harml, String evidence) {
            return new VirusTotalResult(ThreatReputation.UNKNOWN, mal, susp, harml, evidence);
        }

        public static VirusTotalResult unknown(String evidence) {
            return new VirusTotalResult(ThreatReputation.UNKNOWN, 0, 0, 0, evidence);
        }
    }
}
