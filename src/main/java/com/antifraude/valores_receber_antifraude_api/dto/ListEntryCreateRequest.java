package com.antifraude.valores_receber_antifraude_api.dto;

import com.antifraude.valores_receber_antifraude_api.domain.model.enums.ListEntryType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ListEntryCreateRequest {

    @NotNull
    private ListEntryType type; // URL | DOMAIN

    @NotBlank
    private String value; // URL normalizada (sem query) ou domínio / *.dominio.com

    private String reason; // opcional
    private Boolean active; // opcional (default = true)

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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
