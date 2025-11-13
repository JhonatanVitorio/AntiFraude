package com.antifraude.valores_receber_antifraude_api.application.service;

import com.antifraude.valores_receber_antifraude_api.application.service.rules.UrlNormalizer;
import com.antifraude.valores_receber_antifraude_api.domain.model.BlacklistEntry;
import com.antifraude.valores_receber_antifraude_api.domain.model.WhitelistEntry;
import com.antifraude.valores_receber_antifraude_api.domain.model.enums.ListEntryType;
import com.antifraude.valores_receber_antifraude_api.infrastructure.repository.BlacklistRepository;
import com.antifraude.valores_receber_antifraude_api.infrastructure.repository.WhitelistRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class ListsService {

    private final WhitelistRepository whitelistRepository;
    private final BlacklistRepository blacklistRepository;

    public ListsService(WhitelistRepository whitelistRepository,
            BlacklistRepository blacklistRepository) {
        this.whitelistRepository = whitelistRepository;
        this.blacklistRepository = blacklistRepository;
    }

    public MatchResult matchWhitelist(String normalizedUrl, String host) {
        List<WhitelistEntry> list = whitelistRepository.findByActiveTrue();
        for (WhitelistEntry e : list) {
            if (matches(e.getType(), e.getValue(), normalizedUrl, host)) {
                return new MatchResult(true, "WHITELIST_HIT", e.getValue());
            }
        }
        return MatchResult.NO_HIT;
    }

    public MatchResult matchBlacklist(String normalizedUrl, String host) {
        List<BlacklistEntry> list = blacklistRepository.findByActiveTrue();
        for (BlacklistEntry e : list) {
            if (matches(e.getType(), e.getValue(), normalizedUrl, host)) {
                return new MatchResult(true, "BLACKLIST_HIT", e.getValue());
            }
        }
        return MatchResult.NO_HIT;
    }

    private boolean matches(ListEntryType type, String storedValue, String normalizedUrl, String host) {
        if (storedValue == null)
            return false;
        String value = storedValue.trim();

        if (type == ListEntryType.URL) {
            // normaliza o value para comparar URL exata (sem query/fragment)
            UrlNormalizer.Result n = UrlNormalizer.normalize(value);
            return equalsIgnoreCaseSafe(n.normalizedUrl, normalizedUrl);
        }
        // DOMAIN
        String val = value.toLowerCase(Locale.ROOT);
        String h = host != null ? host.toLowerCase(Locale.ROOT) : "";

        if (val.startsWith("*.")) {
            String base = val.substring(2); // "*.dominio.com" -> "dominio.com"
            return h.endsWith("." + base);
        } else {
            // domínio exato
            return equalsIgnoreCaseSafe(val, h);
        }
    }

    private boolean equalsIgnoreCaseSafe(String a, String b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;
        return a.equalsIgnoreCase(b);
    }

    public static class MatchResult {
        public static final MatchResult NO_HIT = new MatchResult(false, null, null);
        public final boolean hit;
        public final String ruleCode; // "WHITELIST_HIT" | "BLACKLIST_HIT"
        public final String matchedValue; // o valor que bateu (para evidência)

        public MatchResult(boolean hit, String ruleCode, String matchedValue) {
            this.hit = hit;
            this.ruleCode = ruleCode;
            this.matchedValue = matchedValue;
        }
    }
}
