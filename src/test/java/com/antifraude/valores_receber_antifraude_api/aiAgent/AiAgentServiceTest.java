package com.antifraude.valores_receber_antifraude_api.aiAgent;

import com.antifraude.valores_receber_antifraude_api.core.model.enums.Verdict;
import com.antifraude.valores_receber_antifraude_api.core.threatintel.ThreatIntelService;
import com.antifraude.valores_receber_antifraude_api.core.threatintel.ThreatIntelService.Reputation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAgentServiceTest {

    @Mock
    private ThreatIntelService threatIntelService;

    @Mock
    private ExternalAiClient externalAiClient;

    @InjectMocks
    private AiAgentService aiAgentService;

    @Test
    void deveClassificarComoSuspect_quandoThreatIntelDizerMalicious() {
        // Arrange
        ThreatIntelService.Result tiResult = new ThreatIntelService.Result();
        tiResult.setReputation(Reputation.MALICIOUS);
        tiResult.addHit("VT_MALICIOUS");
        tiResult.addEvidence("Stub MALICIOUS");

        when(threatIntelService.check(anyString(), anyString()))
                .thenReturn(tiResult);

        // Act
        AiAgentService.Result result = aiAgentService.classify("http://fake.com", "fake.com", 0);

        // Assert
        assertEquals(Verdict.SUSPECT, result.verdict);
        assertEquals("THREAT_INTEL", result.source);
        assertTrue(result.ruleHits.contains("THREAT_INTEL_MALICIOUS"));
        verify(externalAiClient, never()).classify(anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    void deveClassificarComoLegit_quandoThreatIntelCleanERegrasFracas() {
        // Arrange
        ThreatIntelService.Result tiResult = new ThreatIntelService.Result();
        tiResult.setReputation(Reputation.CLEAN);
        tiResult.addHit("THREAT_INTEL_LOCAL_CLEAN");

        when(threatIntelService.check(anyString(), anyString()))
                .thenReturn(tiResult);

        // Act
        AiAgentService.Result result = aiAgentService.classify("https://www.caixa.gov.br", "www.caixa.gov.br", 10);

        // Assert
        assertEquals(Verdict.LEGIT, result.verdict);
        assertEquals("THREAT_INTEL", result.source);
        assertTrue(result.ruleHits.contains("THREAT_INTEL_CLEAN"));
        verify(externalAiClient, never()).classify(anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    void deveCairNaIA_quandoThreatIntelForUnknown_eIAClassificarComoPhishing() {
        // Arrange: ThreatIntel UNKNOWN
        ThreatIntelService.Result tiResult = new ThreatIntelService.Result();
        tiResult.setReputation(Reputation.UNKNOWN);
        tiResult.addHit("THREAT_INTEL_UNKNOWN");

        when(threatIntelService.check(anyString(), anyString()))
                .thenReturn(tiResult);

        // IA dizendo phishing com risco alto
        ExternalAiResponse aiResp = new ExternalAiResponse();
        aiResp.setRiskScore(0.9);
        aiResp.setPhishing(true);
        aiResp.setExplanation("IA detectou padrão de phishing");

        when(externalAiClient.classify(anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(aiResp);

        // Act
        AiAgentService.Result result = aiAgentService.classify("http://simulador-irpf.site", "simulador-irpf.site", 0);

        // Assert
        assertEquals(Verdict.SUSPECT, result.verdict);
        assertEquals("IA", result.source);
        assertTrue(result.ruleHits.contains("IA_PHISHING"));
        assertTrue(result.evidence.stream().anyMatch(e -> e.contains("IA detectou padrão de phishing")));
        verify(threatIntelService, times(1)).check(anyString(), anyString());
        verify(externalAiClient, times(1)).classify(anyString(), anyString(), anyInt(), anyString());
    }
}
