import React from "react";
import type { Theme } from "../../types";

interface ArchitecturePanelProps {
    theme: Theme;
}

const ArchitecturePanel: React.FC<ArchitecturePanelProps> = ({ theme }) => {
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
                Arquitetura da solução & uso de IA
            </h2>

            <div className="grid md:grid-cols-[1.2fr,1fr] gap-5 text-[11px]">
                <div className="space-y-3">
                    <p className={isDark ? "text-slate-200" : "text-slate-800"}>
                        A solução foi desenhada para se aproximar de um módulo antifraude
                        real, porém com foco didático para apresentação em banca:
                    </p>

                    <ul
                        className={`list-disc list-inside space-y-1 ${isDark ? "text-slate-300" : "text-slate-700"
                            }`}
                    >
                        <li>
                            <span className="font-semibold">Frontend (React + TS):</span>{" "}
                            coleta a URL, exibe o veredito, histórico e dashboards
                            explicativos.
                        </li>
                        <li>
                            <span className="font-semibold">API Java / Spring Boot:</span>{" "}
                            expõe o endpoint <code>/api/v1/checks</code>, normaliza a URL,
                            extrai o domínio e orquestra o processo de decisão.
                        </li>
                        <li>
                            <span className="font-semibold">Motor de Regras:</span> aplica
                            validações específicas do golpe de &quot;valores a receber&quot;
                            (similaridade de domínio, palavras-chave, parâmetros suspeitos,
                            histórico).
                        </li>
                        <li>
                            <span className="font-semibold">IA (OpenAI):</span> quando as
                            regras não são suficientes, a IA gera um parecer textual, explica
                            o risco e adiciona evidências à resposta.
                        </li>
                        <li>
                            <span className="font-semibold">
                                Banco de dados (URL Record, whitelist, blacklist):
                            </span>{" "}
                            registra as URLs analisadas e dá contexto para próximas decisões.
                        </li>
                    </ul>

                    <p className={isDark ? "text-slate-400" : "text-slate-600"}>
                        Na apresentação, vocês podem destacar que a arquitetura é extensível:
                        novas regras, novos modelos de IA e novas fontes de dados podem ser
                        plugadas sem mudar o fluxo principal.
                    </p>
                </div>

                <div className="rounded-xl bg-slate-950/70 border border-slate-700 p-3 flex flex-col gap-2 text-[10px]">
                    <div className="mx-auto text-[11px] font-semibold text-slate-200 mb-1">
                        Fluxo resumido
                    </div>

                    <div className="flex flex-col items-center gap-2">
                        <div className="px-3 py-2 rounded-lg bg-slate-900 border border-slate-600 text-center">
                            <p className="font-semibold text-slate-100">Frontend (React)</p>
                            <p className="text-slate-400">
                                Form de URL + visualização de risco
                            </p>
                        </div>

                        <div className="text-slate-500">↓</div>

                        <div className="px-3 py-2 rounded-lg bg-slate-900 border border-slate-600 text-center">
                            <p className="font-semibold text-slate-100">API Spring Boot</p>
                            <p className="text-slate-400">Normalização · Orquestração</p>
                        </div>

                        <div className="flex gap-2 items-center text-slate-500">
                            <span>↙</span>
                            <span>↘</span>
                        </div>

                        <div className="grid grid-cols-2 gap-2 w-full">
                            <div className="px-2 py-2 rounded-lg bg-slate-900 border border-amber-600 text-center">
                                <p className="font-semibold text-amber-100">Motor de Regras</p>
                                <p className="text-amber-200/80">
                                    Padrões do golpe &quot;valores a receber&quot;
                                </p>
                            </div>
                            <div className="px-2 py-2 rounded-lg bg-slate-900 border border-sky-600 text-center">
                                <p className="font-semibold text-sky-100">IA (LLM)</p>
                                <p className="text-sky-200/80">
                                    Explicações & evidências
                                </p>
                            </div>
                        </div>

                        <div className="text-slate-500 mt-1">↓</div>

                        <div className="px-3 py-2 rounded-lg bg-slate-900 border border-emerald-600 text-center">
                            <p className="font-semibold text-emerald-100">Banco de dados</p>
                            <p className="text-emerald-200/80">
                                URL Record · whitelist · blacklist
                            </p>
                        </div>

                        <div className="text-slate-500 mt-1">↓</div>

                        <div className="px-3 py-2 rounded-lg bg-slate-900 border border-slate-600 text-center">
                            <p className="font-semibold text-slate-100">
                                Resposta para o usuário
                            </p>
                            <p className="text-slate-400">
                                Veredito + score + motivos
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ArchitecturePanel;
