export type Verdict = "LEGIT" | "SUSPECT" | "UNKNOWN" | string;

export interface CheckResponse {
    id?: string;
    verdict?: Verdict;
    score?: number | null;
    ruleHits?: string[];
    evidenceSummary?: string[];
    normalizedUrl?: string;
    domain?: string;
    source?: string;
    submittedAt?: string;
    rawInput?: string;
}

export type Theme = "dark" | "light";

export type Tab =
    | "verificacao"
    | "dashboard"
    | "historico"
    | "regras"
    | "arquitetura";

export function formatDate(isoString?: string): string {
    if (!isoString) return "";
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return "";
    return date.toLocaleString("pt-BR");
}

export function normalizeScore(
    score: number | null | undefined
): number | null {
    if (typeof score !== "number") return null;
    if (score >= 0 && score <= 1) return Math.round(score * 100);
    return Math.round(score);
}

export function getVerdictConfig(verdict?: Verdict) {
    const v = (verdict || "").toUpperCase();

    if (v === "SAFE" || v === "LEGIT" || v === "SEGURO") {
        return {
            label: "URL segura",
            description:
                "Nenhum indício relevante de fraude foi encontrado. Ainda assim, sempre confirme o remetente e o canal oficial.",
            badge: "Seguro",
            colorClasses: "border-emerald-400 bg-emerald-950/40 text-emerald-100",
            pillClasses: "bg-emerald-700 text-emerald-100",
        };
    }

    if (v === "SUSPECT" || v === "SUSPICIOUS" || v === "SUSPEITO") {
        return {
            label: "URL suspeita",
            description:
                "Foram detectados sinais de risco. Recomendamos que você não acesse o link e não informe dados pessoais.",
            badge: "Suspeito",
            colorClasses: "border-amber-400 bg-amber-950/40 text-amber-100",
            pillClasses: "bg-amber-700 text-amber-100",
        };
    }

    if (v === "FRAUD" || v === "MALICIOUS" || v === "GOLPE") {
        return {
            label: "Possível golpe",
            description:
                "Alta probabilidade de fraude. Não clique, não compartilhe e oriente o cliente a buscar o canal oficial do banco.",
            badge: "Alto risco",
            colorClasses: "border-red-500 bg-red-950/40 text-red-100",
            pillClasses: "bg-red-700 text-red-100",
        };
    }

    return {
        label: "Classificação indeterminada",
        description:
            "A análise não chegou a um veredito claro. Em casos assim, a recomendação é tratar o link como suspeito.",
        badge: "Indefinido",
        colorClasses: "border-slate-500 bg-slate-900/40 text-slate-100",
        pillClasses: "bg-slate-700 text-slate-100",
    };
}
