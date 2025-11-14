package com.antifraude.valores_receber_antifraude_api.features.check.controller;

import com.antifraude.valores_receber_antifraude_api.features.check.dto.CheckRequest;
import com.antifraude.valores_receber_antifraude_api.features.check.dto.CheckResponse;
import com.antifraude.valores_receber_antifraude_api.features.check.service.CheckService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Checks", description = "Verificação de URLs/mensagens suspeitas")
@RestController
@RequestMapping("/api/v1/checks")
public class CheckController {

    private final CheckService checkService;

    public CheckController(CheckService checkService) {
        this.checkService = checkService;
    }

    @Operation(summary = "Submeter verificação", description = "Recebe uma URL/texto e retorna veredito, score e evidências.")
    @PostMapping
    public ResponseEntity<CheckResponse> submit(@Valid @RequestBody CheckRequest request) {
        return ResponseEntity.ok(checkService.submit(request));
    }
}
