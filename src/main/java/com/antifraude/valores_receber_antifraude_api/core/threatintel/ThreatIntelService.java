package com.antifraude.valores_receber_antifraude_api.core.threatintel;

import java.util.ArrayList;
import java.util.List;

public interface ThreatIntelService {

    /**
     * Analisa a URL usando Threat Intelligence (VT + heurísticas locais).
     */
    Result check(String normalizedUrl, String domain);

    /**
     * Enum compacto de reputação.
     */
    enum Reputation {
        MALICIOUS,
        SUSPICIOUS,
        CLEAN,
        UNKNOWN
    }

    /**
     * Resultado unificado da Threat Intel.
     */
    final class Result {

        private Reputation reputation;
        private final List<String> ruleHits = new ArrayList<>();
        private final List<String> evidence = new ArrayList<>();

        public Result() {
            this.reputation = Reputation.UNKNOWN;
        }

        public Reputation getReputation() {
            return reputation;
        }

        public void setReputation(Reputation reputation) {
            this.reputation = reputation;
        }

        public List<String> getRuleHits() {
            return ruleHits;
        }

        public List<String> getEvidence() {
            return evidence;
        }

        public void addHit(String hit) {
            this.ruleHits.add(hit);
        }

        public void addEvidence(String ev) {
            this.evidence.add(ev);
        }
    }
}
