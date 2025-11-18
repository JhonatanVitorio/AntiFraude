import React from "react";
import type { Theme, CheckResponse } from "../../types";
import { getVerdictConfig, normalizeScore, formatDate } from "../../types";

interface VerificationResultProps {
    theme: Theme;
    loading: boolean;
    result: CheckResponse | null;
}

const VerificationResult: React.FC<VerificationResultProps> = ({
    theme,
    loading,
    result,
}) => {
    const isDark = theme === "dark";

    const subCardBg = isDark
        ? "bg-slate-900/80 border-slate-700"
        : "bg-slate-50 border-slate-200";

    if (!result && !loading) {
        return (
            <div className={`rounded-2xl border p-4 md:p-5 ${subCardBg}`}>
                <h2
                    className={`text-xs font-semibold tracking-[0.14em] uppercase mb-3 ${isDark ? "text-slate-300" : "text-slate-700"
                        }`}
                >
                    Resultado da análise
                </h2>
                <div
                    className={`h-full flex items-center justify-center text-center text-[11px] px-3 ${isDark ? "text-slate-400" : "text-slate-600"
                        }`}
                >
                    Nenhuma URL analisada ainda. Cole um link ao lado e clique em{" "}
                    <span className="font-semibold">“Analisar URL”</span> para ver aqui a
                    classificação de risco.
                </div>
            </div>
        );
    }

    if (loading) {
        return (
            <div className={`rounded-2xl border p-4 md:p-5 ${subCardBg}`}>
                <h2
                    className={`text-xs font-semibold tracking-[0.14em] uppercase mb-3 ${isDark ? "text-slate-300" : "text-slate-700"
                        }`}
                >
                    Resultado da análise
                </h2>
                <div
                    className={`h-full flex flex-col items-center justify-center text-center gap-2 text-[11px] ${isDark ? "text-slate-300" : "text-slate-600"
                        }`}
                >
                    <div className="w-8 h-8 border-2 border-slate-500 border-t-red-500 rounded-full animate-spin" />
                    <p>Rodando motor de regras e IA...</p>
                </div>
            </div>
        );
    }

    if (!result) return null;

    const verdictConfig = getVerdictConfig(result.verdict);
    const score = normalizeScore(result.score);

    const reasons: string[] = [
        ...(result.ruleHits || []).map((r) => `Regra acionada: ${r}`),
        ...(result.evidenceSummary || []).map((e) => `Evidência de IA: ${e}`),
    ];

    return (
        <div className={`rounded-2xl border p-4 md:p-5 ${subCardBg}`}>
            <h2
                className={`text-xs font-semibold tracking-[0.14em] uppercase mb-3 ${isDark ? "text-slate-300" : "text-slate-700"
                    }`}
            >
                Resultado da análise
            </h2>

            <div
                className={`rounded-xl border px-4 py-3 text-xs mt-1 ${verdictConfig.colorClasses}`}
            >
                <div className="flex flex-wrap items-center justify-between gap-2 mb-2">
                    <div className="flex items-center gap-2">
                        <span
                            className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-[10px] font-semibold ${verdictConfig.pillClasses}`}
                        >
                            {verdictConfig.badge}
                        </span>
                        <span className="font-semibold text-[12px]">
                            {verdictConfig.label}
                        </span>
                    </div>

                    {score !== null && (
                        <span className="text-[10px] font-medium">
                            Score de risco:{" "}
                            <span className="font-semibold">{score} / 100</span>
                        </span>
                    )}
                </div>

                <p className="text-[11px]">{verdictConfig.description}</p>

                <div className="mt-3 space-y-1 text-[11px]">
                    {result.rawInput && (
                        <p>
                            <span className="font-medium">URL original: </span>
                            <span className="break-all">{result.rawInput}</span>
                        </p>
                    )}
                    {result.normalizedUrl && (
                        <p>
                            <span className="font-medium">URL normalizada: </span>
                            <span className="break-all">{result.normalizedUrl}</span>
                        </p>
                    )}
                    {result.domain && (
                        <p>
                            <span className="font-medium">Domínio base: </span>
                            {result.domain}
                        </p>
                    )}
                    {result.submittedAt && (
                        <p>
                            <span className="font-medium">Data/hora da análise: </span>
                            {formatDate(result.submittedAt)}
                        </p>
                    )}
                </div>

                {reasons.length > 0 && (
                    <div className="mt-3">
                        <p className="text-[11px] font-semibold mb-1">
                            Motivos identificados:
                        </p>
                        <ul className="list-disc list-inside space-y-0.5 text-[11px]">
                            {reasons.map((reason, idx) => (
                                <li key={idx}>{reason}</li>
                            ))}
                        </ul>
                    </div>
                )}
            </div>
        </div>
    );
};

export default VerificationResult;
