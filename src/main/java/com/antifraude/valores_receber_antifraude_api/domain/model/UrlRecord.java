package com.antifraude.valores_receber_antifraude_api.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import com.antifraude.valores_receber_antifraude_api.domain.model.enums.Verdict;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class UrlRecord {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 2048)
    private String normalizedUrl;

    @Column(nullable = false)
    private String domain;

    @CreationTimestamp
    private LocalDateTime firstSeenAt;

    private LocalDateTime lastSeenAt;

    @Enumerated(EnumType.STRING)
    private Verdict lastStatus;

    private Integer lastScore;

    public UUID getId() {
        return id;
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

    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Verdict getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(Verdict lastStatus) {
        this.lastStatus = lastStatus;
    }

    public Integer getLastScore() {
        return lastScore;
    }

    public void setLastScore(Integer lastScore) {
        this.lastScore = lastScore;
    }
}
