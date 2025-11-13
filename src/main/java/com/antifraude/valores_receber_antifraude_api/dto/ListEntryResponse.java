package com.antifraude.valores_receber_antifraude_api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.antifraude.valores_receber_antifraude_api.domain.model.enums.ListEntryType;

public class ListEntryResponse {
    private UUID id;
    private ListEntryType type;
    private String value; // mapeia a coluna entry_value das entidades
    private boolean active;
    private String reason;
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
