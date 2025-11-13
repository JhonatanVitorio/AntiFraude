package com.antifraude.valores_receber_antifraude_api.shared.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DomainUtils {

    // TLDs comuns em golpes (lista simples, editável)
    private static final Set<String> SUSPICIOUS_TLDS = new HashSet<>(Arrays.asList(
            "xyz", "top", "click", "link", "live", "online", "shop", "buzz", "work", "info"));

    // Encurtadores frequentes
    private static final Set<String> URL_SHORTENERS = new HashSet<>(Arrays.asList(
            "bit.ly", "tinyurl.com", "is.gd", "t.co", "cutt.ly", "linktr.ee", "goo.gl"));

    public static String baseDomain(String host) {
        if (host == null || host.isBlank())
            return "";
        String[] parts = host.toLowerCase().split("\\.");
        if (parts.length <= 2)
            return host.toLowerCase();
        // aproximação simples: pega os dois últimos labels (não cobre todos PSLs)
        String last = parts[parts.length - 1];
        String penult = parts[parts.length - 2];
        return penult + "." + last;
    }

    public static boolean isGovBr(String host) {
        return host != null && host.toLowerCase().endsWith(".gov.br");
    }

    public static boolean isUrlShortener(String host) {
        if (host == null)
            return false;
        return URL_SHORTENERS.contains(host.toLowerCase());
    }

    public static boolean hasSuspiciousTld(String host) {
        if (host == null)
            return false;
        String[] parts = host.toLowerCase().split("\\.");
        String tld = parts[parts.length - 1];
        return SUSPICIOUS_TLDS.contains(tld);
    }

    public static int countLabels(String host) {
        if (host == null || host.isBlank())
            return 0;
        return host.split("\\.").length;
    }

    public static boolean brandInSubdomainButNotOfficialGov(String host) {
        if (host == null)
            return false;
        String h = host.toLowerCase();
        // “gov” ou “govbr” como subdomínio, mas domínio base não é gov.br
        boolean mentionsGov = h.contains("gov");
        return mentionsGov && !isGovBr(host);
    }
}
