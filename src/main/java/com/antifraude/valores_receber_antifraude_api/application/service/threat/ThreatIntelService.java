package com.antifraude.valores_receber_antifraude_api.application.service.threat;

public interface ThreatIntelService {

    /**
     * Analisa a URL usando "inteligência de ameaça" (threat intelligence).
     * Nesta versão, é um stub local, mas a interface está pronta para integrar com
     * APIs externas.
     */
    ThreatIntelResult check(String normalizedUrl, String domain);
}
