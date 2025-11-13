package com.antifraude.valores_receber_antifraude_api.application.service.probe;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class WebFetchService {

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public FetchResult fetch(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "AntifraudeBot/1.0 (+https://jjdev.local)")
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());

            String ct = resp.headers().firstValue("content-type").orElse("");
            String body = "";
            // limite de 1MB para não estourar memória e evitar binários
            if (ct.toLowerCase().contains("text") || ct.toLowerCase().contains("html")) {
                byte[] bytes = resp.body();
                int max = Math.min(bytes.length, 1024 * 1024);
                body = new String(bytes, 0, max);
            }

            return new FetchResult(true, resp.statusCode(), ct, body, resp.uri().toString(), null);
        } catch (Exception e) {
            return new FetchResult(false, 0, null, null, null, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public record FetchResult(boolean ok, int status, String contentType, String body, String finalUrl, String error) {
    }
}
