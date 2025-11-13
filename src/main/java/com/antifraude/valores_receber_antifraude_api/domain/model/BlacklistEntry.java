package com.antifraude.valores_receber_antifraude_api.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import com.antifraude.valores_receber_antifraude_api.domain.model.enums.ListEntryType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "blacklist_entry")
public class BlacklistEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListEntryType type; // URL | DOMAIN

    @Column(name = "entry_value", nullable = false, length = 2048)
    private String value;

    @Column(nullable = false)
    private boolean active = true;

    private String reason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // GETTERS & SETTERS

    public UUID getId() {
        return id;
    }

    public ListEntryType getType() {
        return type;
    }

    public void setType(ListEntryType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
