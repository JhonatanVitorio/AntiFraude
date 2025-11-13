package com.antifraude.valores_receber_antifraude_api.application.service.threat;

import java.util.ArrayList;
import java.util.List;

public class ThreatIntelResult {

    private ThreatReputation reputation;
    private List<String> ruleHits;
    private List<String> evidence;

    public ThreatIntelResult() {
        this.reputation = ThreatReputation.UNKNOWN;
        this.ruleHits = new ArrayList<>();
        this.evidence = new ArrayList<>();
    }

    public ThreatReputation getReputation() {
        return reputation;
    }

    public void setReputation(ThreatReputation reputation) {
        this.reputation = reputation;
    }

    public List<String> getRuleHits() {
        return ruleHits;
    }

    public void setRuleHits(List<String> ruleHits) {
        this.ruleHits = ruleHits;
    }

    public List<String> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<String> evidence) {
        this.evidence = evidence;
    }

    public void addHit(String hit) {
        this.ruleHits.add(hit);
    }

    public void addEvidence(String ev) {
        this.evidence.add(ev);
    }
}
