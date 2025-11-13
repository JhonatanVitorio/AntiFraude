package com.antifraude.valores_receber_antifraude_api.application.service.agent;

public class ExternalAiResponse {

    private Double riskScore; // 0.0 - 1.0
    private Boolean phishing; // true/false
    private String explanation; // texto explicando

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public Boolean getPhishing() {
        return phishing;
    }

    public void setPhishing(Boolean phishing) {
        this.phishing = phishing;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
