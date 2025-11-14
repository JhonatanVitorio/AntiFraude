package com.antifraude.valores_receber_antifraude_api.core.rules;

import java.net.URI;

public class UrlNormalizer {

    public static class Result {
        public final String normalizedUrl;
        public final String domain;

        public Result(String normalizedUrl, String domain) {
            this.normalizedUrl = normalizedUrl;
            this.domain = domain;
        }
    }

    /**
     * Normaliza URLs simples (sem JS/redirect real): força host em minúsculas
     * e remove fragmentos. Se não for URL válida, trata como texto.
     */
    public static Result normalize(String rawInput) {
        try {
            URI uri = new URI(rawInput.trim());
            if (uri.getScheme() == null) {
                // assume http se não tiver esquema
                uri = new URI("http://" + rawInput.trim());
            }
            String host = (uri.getHost() != null) ? uri.getHost().toLowerCase() : "";
            String path = (uri.getPath() != null) ? uri.getPath() : "";
            String normalized = uri.getScheme().toLowerCase() + "://" + host + path;
            return new Result(normalized, host);
        } catch (Exception e) {
            // não é URL: devolve como texto puro
            String raw = rawInput.trim();
            return new Result(raw, "");
        }
    }
}
