package com.antifraude.valores_receber_antifraude_api.features.check.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.antifraude.valores_receber_antifraude_api.core.model.enums.Verdict;

public class CheckResponse {

    private UUID id;
    private Verdict verdict;
    private Integer score;
    private List<String> ruleHits;
    private List<String> evidenceSummary;
    private String normalizedUrl;
    private String domain;
    private String source; // "CACHE" | "RULES" | "IA"
    private LocalDateTime submittedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public List<String> getRuleHits() {
        return ruleHits;
    }

    public void setRuleHits(List<String> ruleHits) {
        this.ruleHits = ruleHits;
    }

    public List<String> getEvidenceSummary() {
        return evidenceSummary;
    }

    public void setEvidenceSummary(List<String> evidenceSummary) {
        this.evidenceSummary = evidenceSummary;
    }

    public String getNormalizedUrl() {
        return normalizedUrl;
    }

    public void setNormalizedUrl(String normalizedUrl) {
        this.normalizedUrl = normalizedUrl;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
