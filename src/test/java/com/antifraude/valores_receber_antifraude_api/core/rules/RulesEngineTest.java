package com.antifraude.valores_receber_antifraude_api.core.rules;

import com.antifraude.valores_receber_antifraude_api.core.model.enums.Verdict;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RulesEngineTest {

    private final RulesEngine rulesEngine = new RulesEngine();

    @Test
    void deveRetornarLegit_quandoNenhumaRegraForAtivada() {
        // Arrange
        String url = "https://www.caixa.gov.br";
        String domain = "www.caixa.gov.br";

        // Act
        RulesEngine.Result result = rulesEngine.evaluate(url, domain);

        // Assert
        assertEquals(Verdict.LEGIT, result.verdict);
        assertEquals(0, result.score);
        assertTrue(result.ruleHits.isEmpty());
        assertTrue(result.evidence.isEmpty());
    }

    @Test
    void deveRetornarUnknown_quandoApenasHttpSemTls() {
        // Arrange
        String url = "http://example.com";
        String domain = "example.com";

        // Act
        RulesEngine.Result result = rulesEngine.evaluate(url, domain);

        // Assert
        assertEquals(Verdict.UNKNOWN, result.verdict);
        assertTrue(result.score > 0);
        assertTrue(result.ruleHits.contains("HTTP_NO_TLS"));
    }

    @Test
    void deveRetornarSuspect_paraFakeShortener() {
        // Arrange
        String url = "http://bit-llly-secure.com";
        String domain = "bit-llly-secure.com";

        // Act
        RulesEngine.Result result = rulesEngine.evaluate(url, domain);

        // Assert
        assertEquals(Verdict.SUSPECT, result.verdict);
        assertTrue(result.score >= 70, "score deveria ser alto para SUSPECT");
        assertTrue(result.ruleHits.contains("FAKE_SHORTENER"));
    }

    @Test
    void deveRetornarSuspect_paraDominiosComSecureEBanking() {
        // Arrange
        String url = "http://banking-secure-auth.com/login";
        String domain = "banking-secure-auth.com";

        // Act
        RulesEngine.Result result = rulesEngine.evaluate(url, domain);

        // Assert
        assertEquals(Verdict.SUSPECT, result.verdict);
        assertTrue(result.ruleHits.contains("SUSPICIOUS_KEYWORD"));
    }
}
