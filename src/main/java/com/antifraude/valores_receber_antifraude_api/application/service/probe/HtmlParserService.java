package com.antifraude.valores_receber_antifraude_api.application.service.probe;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

@Service
public class HtmlParserService {

    public ParsedSignals parse(String html) {
        if (html == null || html.isBlank())
            return new ParsedSignals(null, null, 0, 0, false, false, false);

        Document doc = Jsoup.parse(html);
        String title = doc.title();
        String metaDesc = doc.selectFirst("meta[name=description]") != null
                ? doc.selectFirst("meta[name=description]").attr("content")
                : null;

        Elements forms = doc.select("form");
        boolean hasPassword = !doc.select("input[type=password]").isEmpty();
        boolean mentionsPix = doc.text().toLowerCase().contains("pix");
        boolean mentionsCpf = doc.text().toLowerCase().contains("cpf");

        int links = doc.select("a[href]").size();
        int inputs = doc.select("input,select,textarea").size();

        return new ParsedSignals(title, metaDesc, links, inputs, hasPassword, mentionsPix, mentionsCpf);
    }

    public static record ParsedSignals(
            String title,
            String metaDescription,
            int linkCount,
            int inputCount,
            boolean hasPasswordField,
            boolean mentionsPix,
            boolean mentionsCpf) {
    }
}
