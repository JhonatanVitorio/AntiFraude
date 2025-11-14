package com.antifraude.valores_receber_antifraude_api.lists.controller;

import com.antifraude.valores_receber_antifraude_api.core.model.entity.BlacklistEntry;
import com.antifraude.valores_receber_antifraude_api.core.repository.BlacklistRepository;
import com.antifraude.valores_receber_antifraude_api.lists.dto.ListEntryCreateRequest;
import com.antifraude.valores_receber_antifraude_api.lists.dto.ListEntryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Blacklist", description = "Gerenciamento de domínios/URLs suspeitos")
@RestController
@RequestMapping("/api/v1/blacklist")
public class BlacklistController {

    private final BlacklistRepository blacklistRepository;

    public BlacklistController(BlacklistRepository blacklistRepository) {
        this.blacklistRepository = blacklistRepository;
    }

    @Operation(summary = "Listar entradas", description = "Retorna todas as entradas. Use ?active=true para apenas ativas.")
    @GetMapping
    public ResponseEntity<List<ListEntryResponse>> list(@RequestParam(required = false) Boolean active) {
        List<BlacklistEntry> entries = active == null ? blacklistRepository.findAll()
                : (active ? blacklistRepository.findByActiveTrue()
                        : blacklistRepository.findAll().stream().filter(e -> !e.isActive()).toList());

        return ResponseEntity.ok(entries.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "Criar entrada", description = "Adiciona uma entrada na blacklist (URL exata ou domínio).")
    @PostMapping
    public ResponseEntity<ListEntryResponse> create(@Valid @RequestBody ListEntryCreateRequest req) {
        BlacklistEntry e = new BlacklistEntry();
        e.setType(req.getType());
        e.setValue(req.getValue());
        e.setReason(req.getReason());
        e.setActive(req.getActive() == null ? true : req.getActive());
        e = blacklistRepository.save(e);

        ListEntryResponse resp = toResponse(e);
        return ResponseEntity.created(URI.create("/api/v1/blacklist/" + e.getId())).body(resp);
    }

    @Operation(summary = "Desativar (soft delete)", description = "Marca a entrada como inativa.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        var e = blacklistRepository.findById(id).orElse(null);
        if (e == null)
            return ResponseEntity.notFound().build();
        e.setActive(false);
        blacklistRepository.save(e);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ativar entrada")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ListEntryResponse> activate(@PathVariable UUID id) {
        var e = blacklistRepository.findById(id).orElse(null);
        if (e == null)
            return ResponseEntity.notFound().build();
        e.setActive(true);
        e = blacklistRepository.save(e);
        return ResponseEntity.ok(toResponse(e));
    }

    @Operation(summary = "Desativar entrada")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ListEntryResponse> deactivate(@PathVariable UUID id) {
        var e = blacklistRepository.findById(id).orElse(null);
        if (e == null)
            return ResponseEntity.notFound().build();
        e.setActive(false);
        e = blacklistRepository.save(e);
        return ResponseEntity.ok(toResponse(e));
    }

    private ListEntryResponse toResponse(BlacklistEntry e) {
        ListEntryResponse r = new ListEntryResponse();
        r.setId(e.getId());
        r.setType(e.getType());
        r.setValue(e.getValue());
        r.setActive(e.isActive());
        r.setReason(e.getReason());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }
}
