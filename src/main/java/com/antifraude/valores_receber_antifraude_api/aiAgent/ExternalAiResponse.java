package com.antifraude.valores_receber_antifraude_api.aiAgent;


public class ExternalAiResponse {

    private Double riskScore; // 0.0 a 1.0
    private Boolean phishing; // true = golpe, false = ok
    private String explanation; // texto explicando a decisão

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
