package com.antifraude.valores_receber_antifraude_api.core.threatintel;

import org.junit.jupiter.api.Test;

import static com.antifraude.valores_receber_antifraude_api.core.threatintel.ThreatIntelService.Reputation;
import static org.junit.jupiter.api.Assertions.*;

class ThreatIntelServiceImplTest {

    @Test
    void deveUsarVirusTotalQuandoReputacaoNaoForUnknown() {
        // Arrange: VT sempre MALICIOUS
        ThreatIntelServiceImpl.VirusTotalClient vtStub = new ThreatIntelServiceImpl.VirusTotalClient() {
            @Override
            public VirusTotalResult checkUrl(String normalizedUrl) {
                return new VirusTotalResult(
                        Reputation.MALICIOUS,
                        10,
                        0,
                        0,
                        "Stub MALICIOUS");
            }
        };

        ThreatIntelServiceImpl service = new ThreatIntelServiceImpl(vtStub);

        // Act
        ThreatIntelService.Result result = service.check("http://qualquercoisa.com", "qualquercoisa.com");

        // Assert
        assertEquals(Reputation.MALICIOUS, result.getReputation());
        assertTrue(result.getRuleHits().contains("VT_MALICIOUS"));
        assertFalse(result.getEvidence().isEmpty());
    }

    @Test
    void deveUsarHeuristicaLocal_paraFakeWhatsapp() {
        // Arrange: VT sempre UNKNOWN
        ThreatIntelServiceImpl.VirusTotalClient vtStub = new ThreatIntelServiceImpl.VirusTotalClient() {
            @Override
            public VirusTotalResult checkUrl(String normalizedUrl) {
                return VirusTotalResult.unknown();
            }
        };

        ThreatIntelServiceImpl service = new ThreatIntelServiceImpl(vtStub);

        // Act
        ThreatIntelService.Result result = service.check("http://whatsap-confirmacao.com", "whatsap-confirmacao.com");

        // Assert
        assertEquals(Reputation.MALICIOUS, result.getReputation());
        assertTrue(result.getRuleHits().contains("THREAT_INTEL_TYPO_WHATSAPP"));
    }

    @Test
    void deveMarcarClean_paraDominioConfiavel() {
        // Arrange: VT sempre UNKNOWN
        ThreatIntelServiceImpl.VirusTotalClient vtStub = new ThreatIntelServiceImpl.VirusTotalClient() {
            @Override
            public VirusTotalResult checkUrl(String normalizedUrl) {
                return VirusTotalResult.unknown();
            }
        };

        ThreatIntelServiceImpl service = new ThreatIntelServiceImpl(vtStub);

        // Act
        ThreatIntelService.Result result = service.check("https://www.caixa.gov.br/minha-conta", "www.caixa.gov.br");

        // Assert
        assertEquals(Reputation.CLEAN, result.getReputation());
        assertTrue(result.getRuleHits().contains("THREAT_INTEL_LOCAL_CLEAN"));
    }
}
