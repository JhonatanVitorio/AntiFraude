package com.antifraude.valores_receber_antifraude_api.testsupport;

import com.antifraude.valores_receber_antifraude_api.features.check.dto.CheckRequest;
import com.antifraude.valores_receber_antifraude_api.features.check.dto.InputType;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static final String SUSPICIOUS_URL = "http://simulador-irpf.site";
    public static final String LEGIT_URL = "https://www.caixa.gov.br";

    public static CheckRequest suspiciousCheckRequest() {
        CheckRequest req = new CheckRequest();
        req.setRawInput(SUSPICIOUS_URL);

        // ENUM correto
        req.setInputType(InputType.URL);

        return req;
    }

    public static CheckRequest legitCheckRequest() {
        CheckRequest req = new CheckRequest();
        req.setRawInput(LEGIT_URL);

        // ENUM correto
        req.setInputType(InputType.URL);

        return req;
    }
}
