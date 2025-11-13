package com.antifraude.valores_receber_antifraude_api.application.service.threat;

/**
 * DTO genérico para mapear a resposta de uma API externa de Threat
 * Intelligence.
 * 
 * Adapte os campos conforme a API que você escolher usar.
 */
public class ExternalThreatIntelResponse {

    private String reputation; // "MALICIOUS", "SUSPICIOUS", "CLEAN", "UNKNOWN"
    private Integer score; // 0-100, opcional
    private String provider; // nome do provedor, se existir
    private String reason; // texto explicando o motivo

    public String getReputation() {
        return reputation;
    }

    public void setReputation(String reputation) {
        this.reputation = reputation;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
