import React from "react";
import type { Theme, CheckResponse } from "../../types";

interface RiskDashboardProps {
    theme: Theme;
    history: CheckResponse[];
}

const RiskDashboard: React.FC<RiskDashboardProps> = ({ theme, history }) => {
    const isDark = theme === "dark";

    const subCardBg = isDark
        ? "bg-slate-900/80 border-slate-700"
        : "bg-slate-50 border-slate-200";

    const total = history.length || 1;
    const safeCount = history.filter(
        (h) => (h.verdict || "").toUpperCase() === "LEGIT"
    ).length;
    const suspectCount = history.filter(
        (h) => (h.verdict || "").toUpperCase() === "SUSPECT"
    ).length;
    const fraudCount = history.filter(
        (h) => (h.verdict || "").toUpperCase() === "FRAUD"
    ).length;

    const safePct = Math.round((safeCount / total) * 100);
    const suspectPct = Math.round((suspectCount / total) * 100);
    const fraudPct = Math.round((fraudCount / total) * 100);

    return (
        <div className={`rounded-2xl border p-4 md:p-5 ${subCardBg}`}>
            <h2
                className={`text-xs font-semibold tracking-[0.14em] uppercase mb-4 ${isDark ? "text-slate-300" : "text-slate-700"
                    }`}
            >
                Visão geral do risco
            </h2>

            <div className="grid gap-3 md:grid-cols-3 mb-6 text-xs">
                <div
                    className={`rounded-xl p-3 ${isDark
                            ? "bg-slate-950/50 border border-slate-700"
                            : "bg-white border border-slate-200"
                        }`}
                >
                    <p
                        className={`text-[11px] ${isDark ? "text-slate-400" : "text-slate-500"
                            }`}
                    >
                        Total de URLs
                    </p>
                    <p
                        className={`text-xl font-semibold mt-1 ${isDark ? "text-slate-50" : "text-slate-900"
                            }`}
                    >
                        {history.length}
                    </p>
                    <p
                        className={`text-[11px] mt-1 ${isDark ? "text-slate-500" : "text-slate-500"
                            }`}
                    >
                        Links analisados nesta sessão
                    </p>
                </div>
                <div className="rounded-xl bg-emerald-950/40 border border-emerald-600/70 p-3">
                    <p className="text-[11px] text-emerald-200">Seguras</p>
                    <p className="text-xl font-semibold mt-1 text-emerald-100">
                        {safeCount}{" "}
                        <span className="text-[11px] font-normal">
                            ({isNaN(safePct) ? 0 : safePct}%)
                        </span>
                    </p>
                </div>
                <div className="rounded-xl bg-red-950/40 border border-red-600/70 p-3">
                    <p className="text-[11px] text-red-200">Suspeitas / Golpes</p>
                    <p className="text-xl font-semibold mt-1 text-red-100">
                        {suspectCount + fraudCount}{" "}
                        <span className="text-[11px] font-normal">
                            (
                            {isNaN(fraudPct + suspectPct)
                                ? 0
                                : fraudPct + suspectPct}
                            %)
                        </span>
                    </p>
                </div>
            </div>

            <div
                className={`text-[11px] mb-2 ${isDark ? "text-slate-300" : "text-slate-700"
                    }`}
            >
                Distribuição por classificação
            </div>
            <div className="flex items-end gap-4 h-40 text-[11px]">
                <div className="flex-1 flex flex-col items-center">
                    <div
                        className="w-8 rounded-t-md bg-emerald-500/80"
                        style={{ height: `${safePct || 4}%` }}
                    />
                    <span
                        className={`mt-2 ${isDark ? "text-slate-300" : "text-slate-700"
                            }`}
                    >
                        Seguro
                    </span>
                </div>
                <div className="flex-1 flex flex-col items-center">
                    <div
                        className="w-8 rounded-t-md bg-amber-400/80"
                        style={{ height: `${suspectPct || 4}%` }}
                    />
                    <span
                        className={`mt-2 ${isDark ? "text-slate-300" : "text-slate-700"
                            }`}
                    >
                        Suspeito
                    </span>
                </div>
                <div className="flex-1 flex flex-col items-center">
                    <div
                        className="w-8 rounded-t-md bg-red-500/80"
                        style={{ height: `${fraudPct || 4}%` }}
                    />
                    <span
                        className={`mt-2 ${isDark ? "text-slate-300" : "text-slate-700"
                            }`}
                    >
                        Golpe
                    </span>
                </div>
            </div>

            <p
                className={`text-[10px] mt-4 ${isDark ? "text-slate-400" : "text-slate-600"
                    }`}
            >
                Este dashboard é focado na demonstração do comportamento da solução
                durante a apresentação: à medida que a banca testa novas URLs, a
                distribuição vai sendo atualizada.
            </p>
        </div>
    );
};

export default RiskDashboard;
