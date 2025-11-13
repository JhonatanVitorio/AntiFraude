package com.antifraude.valores_receber_antifraude_api.api.controller;

import com.antifraude.valores_receber_antifraude_api.domain.model.WhitelistEntry;
import com.antifraude.valores_receber_antifraude_api.dto.ListEntryCreateRequest;
import com.antifraude.valores_receber_antifraude_api.dto.ListEntryResponse;
import com.antifraude.valores_receber_antifraude_api.infrastructure.repository.WhitelistRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Whitelist", description = "Gerenciamento de domínios/URLs confiáveis")
@RestController
@RequestMapping("/api/v1/whitelist")
public class WhitelistController {

    private final WhitelistRepository whitelistRepository;

    public WhitelistController(WhitelistRepository whitelistRepository) {
        this.whitelistRepository = whitelistRepository;
    }

    @Operation(summary = "Listar entradas", description = "Retorna todas as entradas. Use ?active=true para apenas ativas.")
    @GetMapping
    public ResponseEntity<List<ListEntryResponse>> list(@RequestParam(required = false) Boolean active) {
        List<WhitelistEntry> entries = active == null ? whitelistRepository.findAll()
                : (active ? whitelistRepository.findByActiveTrue()
                        : whitelistRepository.findAll().stream().filter(e -> !e.isActive()).toList());

        return ResponseEntity.ok(entries.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "Criar entrada", description = "Adiciona uma entrada na whitelist (URL exata ou domínio).")
    @PostMapping
    public ResponseEntity<ListEntryResponse> create(@Valid @RequestBody ListEntryCreateRequest req) {
        WhitelistEntry e = new WhitelistEntry();
        e.setType(req.getType());
        e.setValue(req.getValue());
        e.setReason(req.getReason());
        e.setActive(req.getActive() == null ? true : req.getActive());
        e = whitelistRepository.save(e);

        ListEntryResponse resp = toResponse(e);
        return ResponseEntity.created(URI.create("/api/v1/whitelist/" + e.getId())).body(resp);
    }

    @Operation(summary = "Desativar (soft delete)", description = "Marca a entrada como inativa.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        var e = whitelistRepository.findById(id).orElse(null);
        if (e == null)
            return ResponseEntity.notFound().build();
        e.setActive(false);
        whitelistRepository.save(e);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ativar entrada")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ListEntryResponse> activate(@PathVariable UUID id) {
        var e = whitelistRepository.findById(id).orElse(null);
        if (e == null)
            return ResponseEntity.notFound().build();
        e.setActive(true);
        e = whitelistRepository.save(e);
        return ResponseEntity.ok(toResponse(e));
    }

    @Operation(summary = "Desativar entrada")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ListEntryResponse> deactivate(@PathVariable UUID id) {
        var e = whitelistRepository.findById(id).orElse(null);
        if (e == null)
            return ResponseEntity.notFound().build();
        e.setActive(false);
        e = whitelistRepository.save(e);
        return ResponseEntity.ok(toResponse(e));
    }

    private ListEntryResponse toResponse(WhitelistEntry e) {
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
