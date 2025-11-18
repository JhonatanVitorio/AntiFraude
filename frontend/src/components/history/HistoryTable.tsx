import React from "react";
import type { Theme, CheckResponse } from "../../types";
import { formatDate, normalizeScore } from "../../types";

interface HistoryTableProps {
    theme: Theme;
    history: CheckResponse[];
}

const HistoryTable: React.FC<HistoryTableProps> = ({ theme, history }) => {
    const isDark = theme === "dark";

    const subCardBg = isDark
        ? "bg-slate-900/80 border-slate-700"
        : "bg-slate-50 border-slate-200";

    return (
        <div className={`rounded-2xl border p-4 md:p-5 ${subCardBg}`}>
            <h2
                className={`text-xs font-semibold tracking-[0.14em] uppercase mb-4 ${isDark ? "text-slate-300" : "text-slate-700"
                    }`}
            >
                Histórico de verificações (sessão atual)
            </h2>

            {history.length === 0 ? (
                <p
                    className={`text-[11px] ${isDark ? "text-slate-400" : "text-slate-600"
                        }`}
                >
                    Ainda não há verificações nesta sessão. Use a aba de{" "}
                    <span className="font-semibold">Verificação em tempo real</span> para
                    iniciar.
                </p>
            ) : (
                <div className="overflow-x-auto text-[11px]">
                    <table className="min-w-full border-separate border-spacing-y-1">
                        <thead
                            className={isDark ? "text-slate-400" : "text-slate-600"}
                        >
                            <tr>
                                <th className="text-left py-1 px-2">Data/Hora</th>
                                <th className="text-left py-1 px-2">URL</th>
                                <th className="text-left py-1 px-2">Domínio</th>
                                <th className="text-left py-1 px-2">Veredito</th>
                                <th className="text-left py-1 px-2">Score</th>
                            </tr>
                        </thead>
                        <tbody>
                            {history.map((item, idx) => {
                                const v = (item.verdict || "").toUpperCase();
                                let badgeClass =
                                    "bg-slate-700 text-slate-100 border-slate-500";
                                if (v === "SAFE")
                                    badgeClass =
                                        "bg-emerald-700 text-emerald-100 border-emerald-500";
                                if (v === "SUSPECT")
                                    badgeClass =
                                        "bg-amber-700 text-amber-100 border-amber-500";
                                if (v === "FRAUD")
                                    badgeClass = "bg-red-700 text-red-100 border-red-500";

                                const itemScore = normalizeScore(item.score);

                                return (
                                    <tr key={idx}>
                                        <td
                                            className={`py-1 px-2 ${isDark ? "text-slate-300" : "text-slate-800"
                                                }`}
                                        >
                                            {formatDate(item.submittedAt)}
                                        </td>
                                        <td
                                            className={`py-1 px-2 max-w-[200px] truncate ${isDark ? "text-slate-200" : "text-slate-900"
                                                }`}
                                        >
                                            {item.rawInput}
                                        </td>
                                        <td
                                            className={`py-1 px-2 ${isDark ? "text-slate-300" : "text-slate-800"
                                                }`}
                                        >
                                            {item.domain || "-"}
                                        </td>
                                        <td className="py-1 px-2">
                                            <span
                                                className={`inline-flex items-center px-2 py-0.5 rounded-full border text-[10px] ${badgeClass}`}
                                            >
                                                {v || "UNKNOWN"}
                                            </span>
                                        </td>
                                        <td
                                            className={`py-1 px-2 ${isDark ? "text-slate-300" : "text-slate-800"
                                                }`}
                                        >
                                            {itemScore !== null ? `${itemScore}/100` : "-"}
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            )}

            <p
                className={`text-[10px] mt-3 ${isDark ? "text-slate-400" : "text-slate-600"
                    }`}
            >
                O histórico demonstra como o módulo registra cada verificação,
                permitindo auditoria, ajustes de regras e evolução da IA com base nos
                casos reais analisados.
            </p>
        </div>
    );
};

export default HistoryTable;
