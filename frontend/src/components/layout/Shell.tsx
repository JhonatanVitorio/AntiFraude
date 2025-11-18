import React from "react";
import type { Theme, Tab } from "../../types";
import Header from "./Header";
import Tabs from "./Tabs";

interface ShellProps {
    theme: Theme;
    onToggleTheme: () => void;
    activeTab: Tab;
    onChangeTab: (tab: Tab) => void;
    children: React.ReactNode;
}

const Shell: React.FC<ShellProps> = ({
    theme,
    onToggleTheme,
    activeTab,
    onChangeTab,
    children,
}) => {
    const isDark = theme === "dark";

    const layoutBg = isDark
        ? "bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 text-slate-100"
        : "bg-slate-100 text-slate-900";

    const cardBg = isDark
        ? "bg-slate-900/90 border-slate-700"
        : "bg-white border-slate-200";

    return (
        <div
            className={`min-h-screen flex items-center justify-center relative overflow-hidden px-4 py-8 transition-colors ${layoutBg}`}
        >
            {/* Fundo com shapes minimalistas */}
            <div className="pointer-events-none absolute inset-0">
                {isDark ? (
                    <>
                        <div className="absolute w-[650px] h-[650px] bg-red-700/20 blur-[140px] -top-40 -left-40 rounded-full" />
                        <div className="absolute w-[550px] h-[550px] bg-blue-600/25 blur-[150px] bottom-[-10rem] right-[-8rem] rounded-full" />
                        <div className="absolute inset-0 bg-[url('https://www.transparenttextures.com/patterns/cubes.png')] opacity-[0.04]" />
                        <div className="absolute inset-y-0 left-1/4 w-px bg-gradient-to-b from-transparent via-slate-700/50 to-transparent" />
                        <div className="absolute inset-y-0 right-1/5 w-px bg-gradient-to-b from-transparent via-slate-700/40 to-transparent" />
                    </>
                ) : (
                    <>
                        <div className="absolute w-[650px] h-[650px] bg-red-400/15 blur-[150px] -top-40 -left-32 rounded-full" />
                        <div className="absolute w-[550px] h-[550px] bg-blue-400/15 blur-[160px] bottom-[-10rem] right-[-8rem] rounded-full" />
                        <div className="absolute inset-0 bg-[url('https://www.transparenttextures.com/patterns/cubes.png')] opacity-[0.05]" />
                        <div className="absolute inset-y-0 left-1/4 w-px bg-gradient-to-b from-transparent via-slate-300/60 to-transparent" />
                        <div className="absolute inset-y-0 right-1/5 w-px bg-gradient-to-b from-transparent via-slate-300/50 to-transparent" />
                    </>
                )}
            </div>

            <div className="relative w-full max-w-6xl">
                <div
                    className={`rounded-3xl border shadow-[0_26px_90px_rgba(0,0,0,0.55)] px-6 py-6 md:px-10 md:py-8 ${cardBg}`}
                >
                    <Header theme={theme} onToggleTheme={onToggleTheme} />

                    {/* Título / subtítulo / chips / botão saiba mais */}
                    <div className="text-center mb-6 space-y-2">
                        <h1
                            className={`text-2xl md:text-3xl font-semibold tracking-tight ${isDark ? "text-slate-50" : "text-slate-900"
                                }`}
                        >
                            Verificação inteligente de links de &quot;valores a receber&quot;
                        </h1>
                        <p
                            className={`text-[12px] md:text-[13px] max-w-2xl mx-auto ${isDark ? "text-slate-400" : "text-slate-600"
                                }`}
                        >
                            Antes de o usuário clicar, o módulo valida a URL combinando{" "}
                            <span
                                className={
                                    isDark ? "font-medium text-slate-200" : "font-medium text-slate-900"
                                }
                            >
                                IA generativa
                            </span>
                            ,{" "}
                            <span
                                className={
                                    isDark ? "font-medium text-slate-200" : "font-medium text-slate-900"
                                }
                            >
                                motor de regras antifraude
                            </span>{" "}
                            e{" "}
                            <span
                                className={
                                    isDark ? "font-medium text-slate-200" : "font-medium text-slate-900"
                                }
                            >
                                base de whitelist/blacklist
                            </span>{" "}
                            focada no golpe de &quot;valores a receber&quot;.
                        </p>

                        <div className="flex flex-wrap justify-center gap-2 mt-2">
                            <span
                                className={`px-2.5 py-1 rounded-full text-[11px] border ${isDark
                                    ? "border-slate-600 text-slate-300 bg-slate-900/60"
                                    : "border-slate-300 text-slate-700 bg-slate-100"
                                    }`}
                            >
                                IA treinada no contexto de golpes bancários
                            </span>
                            <span
                                className={`px-2.5 py-1 rounded-full text-[11px] border ${isDark
                                    ? "border-slate-600 text-slate-300 bg-slate-900/60"
                                    : "border-slate-300 text-slate-700 bg-slate-100"
                                    }`}
                            >
                                Motor de regras simula time de risco
                            </span>
                            <span
                                className={`px-2.5 py-1 rounded-full text-[11px] border ${isDark
                                    ? "border-slate-600 text-slate-300 bg-slate-900/60"
                                    : "border-slate-300 text-slate-700 bg-slate-100"
                                    }`}
                            >
                                Persistência de histórico de URLs
                            </span>

                            <button
                                type="button"
                                onClick={() => onChangeTab("arquitetura")}
                                className={`
        mt-1 inline-flex items-center gap-1 rounded-full border text-[11px] font-medium px-3 py-1 transition-colors shadow-sm
        
        ${isDark
                                        ? "border-red-500/80 text-red-200 bg-red-600/10 hover:bg-red-600 hover:text-white"
                                        : "border-red-500/80 text-red-600 bg-white hover:bg-red-100 hover:text-red-700"
                                    }
    `}
                            >
                                Saiba mais sobre o projeto
                            </button>
                        </div>
                    </div>

                    <Tabs theme={theme} activeTab={activeTab} onChangeTab={onChangeTab} />

                    {/* Conteúdo da aba */}
                    {children}

                    {/* Rodapé */}
                    <div className="mt-6 flex flex-wrap items-center justify-between gap-2 text-[10px]">
                        <span className={isDark ? "text-slate-500" : "text-slate-600"}>
                            Projeto acadêmico – Módulo antifraude focado no golpe de &quot;valores
                            a receber&quot;.
                        </span>
                        <span
                            className={isDark ? "text-slate-500/80" : "text-slate-600/80"}
                        >
                            Desenvolvido por Jhonatan Vitorio e equipe ·{" "}
                            {new Date().getFullYear()}
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Shell;
