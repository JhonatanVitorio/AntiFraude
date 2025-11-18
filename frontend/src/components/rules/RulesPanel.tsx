import React from "react";
import type { Theme } from "../../types";

interface RulesPanelProps {
    theme: Theme;
}

const RulesPanel: React.FC<RulesPanelProps> = ({ theme }) => {
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
                Regras antifraude & alertas
            </h2>

            <div className="grid md:grid-cols-2 gap-4 text-[11px]">
                <div className="rounded-xl bg-slate-950/60 border border-slate-700 p-3">
                    <p className="text-[11px] font-semibold text-slate-100 mb-1">
                        1. Domínio similar ao do banco
                    </p>
                    <p className="text-slate-300">
                        Ex.: &quot;bradescco.com&quot;, &quot;bradesco-valores.com&quot;,
                        etc. Dispara alerta de similaridade e aumenta o score de risco.
                    </p>
                </div>
                <div className="rounded-xl bg-slate-950/60 border border-slate-700 p-3">
                    <p className="text-[11px] font-semibold text-slate-100 mb-1">
                        2. Palavras-chave típicas de golpe
                    </p>
                    <p className="text-slate-300">
                        Termos como &quot;valores a receber&quot;, &quot;saldo bloqueado&quot; e
                        &quot;regularização urgente&quot; aumentam o peso da análise.
                    </p>
                </div>
                <div className="rounded-xl bg-slate-950/60 border border-slate-700 p-3">
                    <p className="text-[11px] font-semibold text-slate-100 mb-1">
                        3. URLs recém-criadas / pouco vistas
                    </p>
                    <p className="text-slate-300">
                        Endereços sem histórico na base ou sem recorrência entram como
                        risco intermediário, priorizando o olho humano.
                    </p>
                </div>
                <div className="rounded-xl bg-slate-950/60 border border-slate-700 p-3">
                    <p className="text-[11px] font-semibold text-slate-100 mb-1">
                        4. Padrões suspeitos de parâmetros
                    </p>
                    <p className="text-slate-300">
                        Parâmetros com CPF, token ou dados sensíveis na URL indicam risco
                        alto de vazamento de informações.
                    </p>
                </div>
            </div>

            <p
                className={`text-[10px] mt-4 ${isDark ? "text-slate-400" : "text-slate-600"
                    }`}
            >
                O motor de regras funciona como a primeira linha de defesa, traduzindo
                conhecimento de especialistas de risco em validações automatizadas, e a
                IA entra como segunda camada, ajudando nas situações ambíguas.
            </p>
        </div>
    );
};

export default RulesPanel;
