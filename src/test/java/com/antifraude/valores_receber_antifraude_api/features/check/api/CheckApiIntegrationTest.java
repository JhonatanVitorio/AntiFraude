package com.antifraude.valores_receber_antifraude_api.features.check.api;

import com.antifraude.valores_receber_antifraude_api.core.model.entity.BlacklistEntry;
import com.antifraude.valores_receber_antifraude_api.core.model.entity.UrlRecord;
import com.antifraude.valores_receber_antifraude_api.core.model.entity.WhitelistEntry;
import com.antifraude.valores_receber_antifraude_api.core.model.enums.Verdict;
import com.antifraude.valores_receber_antifraude_api.core.repository.BlacklistRepository;
import com.antifraude.valores_receber_antifraude_api.core.repository.UrlRecordRepository;
import com.antifraude.valores_receber_antifraude_api.core.repository.WhitelistRepository;
import com.antifraude.valores_receber_antifraude_api.features.check.dto.CheckRequest;
import com.antifraude.valores_receber_antifraude_api.testsupport.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@Transactional
public class CheckApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UrlRecordRepository urlRecordRepository;

    @Autowired
    private BlacklistRepository blacklistRepository;

    @Autowired
    private WhitelistRepository whitelistRepository;

    @BeforeEach
    void limparBanco() {
        blacklistRepository.deleteAll();
        whitelistRepository.deleteAll();
        urlRecordRepository.deleteAll();
    }

    @Test
    void postDeveRetornarSuspect_ePersistirEmUrlRecordEBlacklist() throws Exception {
        CheckRequest req = TestDataFactory.suspiciousCheckRequest();

        String json = objectMapper.writeValueAsString(req);

        // chama o endpoint real (ajusta a URL se for diferente no seu controller)
        var mvcResult = mockMvc.perform(post("/api/v1/checks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.verdict", is("SUSPECT")))
                .andReturn();

        // verifica se a URL foi salva em UrlRecord
        String normalizedUrl = TestDataFactory.SUSPICIOUS_URL;
        Optional<UrlRecord> optRecord = urlRecordRepository.findByNormalizedUrl(normalizedUrl);
        assertTrue(optRecord.isPresent(), "UrlRecord deveria ter sido salvo para URL suspeita");
        assertEquals(Verdict.SUSPECT, optRecord.get().getLastStatus());

        // verifica entrada na blacklist
        List<BlacklistEntry> blacks = blacklistRepository.findAll();
        assertEquals(1, blacks.size(), "Deveria haver uma entrada na blacklist");
        assertTrue(blacks.get(0).getValue().contains("simulador-irpf.site"));
    }

    @Test
    void postDeveRetornarLegit_ePersistirEmUrlRecordEWhitelist() throws Exception {
        CheckRequest req = TestDataFactory.legitCheckRequest();

        String json = objectMapper.writeValueAsString(req);

        var mvcResult = mockMvc.perform(post("/api/v1/checks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.verdict", is("LEGIT")))
                .andReturn();

        String normalizedUrl = TestDataFactory.LEGIT_URL;
        Optional<UrlRecord> optRecord = urlRecordRepository.findByNormalizedUrl(normalizedUrl);
        assertTrue(optRecord.isPresent(), "UrlRecord deveria ter sido salvo para URL legítima");
        assertEquals(Verdict.LEGIT, optRecord.get().getLastStatus());

        List<WhitelistEntry> whites = whitelistRepository.findAll();
        assertEquals(1, whites.size(), "Deveria haver uma entrada na whitelist");
        assertTrue(whites.get(0).getValue().contains("caixa.gov.br"));
    }
}
