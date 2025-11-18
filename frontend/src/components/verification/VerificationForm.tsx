import React from "react";
import type { Theme } from "../../types";

interface VerificationFormProps {
    theme: Theme;
    url: string;
    error: string | null;
    loading: boolean;
    onUrlChange: (value: string) => void;
    onSubmit: (e: React.FormEvent<HTMLFormElement>) => void;
}

const VerificationForm: React.FC<VerificationFormProps> = ({
    theme,
    url,
    error,
    loading,
    onUrlChange,
    onSubmit,
}) => {
    const isDark = theme === "dark";

    const subCardBg = isDark
        ? "bg-slate-900/80 border-slate-700"
        : "bg-slate-50 border-slate-200";

    return (
        <div className={`rounded-2xl border p-4 md:p-5 ${subCardBg}`}>
            <h2
                className={`text-xs font-semibold tracking-[0.14em] uppercase mb-3 ${isDark ? "text-slate-300" : "text-slate-700"
                    }`}
            >
                Analisar URL
            </h2>

            <form onSubmit={onSubmit} className="space-y-3">
                <div className="space-y-1.5">
                    <label
                        htmlFor="url"
                        className={`text-[11px] font-medium ${isDark ? "text-slate-300" : "text-slate-700"
                            }`}
                    >
                        URL recebida
                    </label>
                    <div className="relative">
                        <span
                            className={`absolute left-3 top-1/2 -translate-y-1/2 text-xs ${isDark ? "text-slate-500" : "text-slate-500"
                                }`}
                        >
                            🔗
                        </span>
                        <input
                            id="url"
                            type="text"
                            value={url}
                            onChange={(e) => onUrlChange(e.target.value)}
                            autoComplete="off"
                            placeholder="https://exemplo.com.br/valores-a-receber"
                            className={
                                "w-full rounded-xl pl-8 pr-3 py-2.5 text-xs md:text-sm focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-red-500 border " +
                                (isDark
                                    ? "bg-slate-950/60 border-slate-600 text-slate-50 placeholder:text-slate-500"
                                    : "bg-white border-slate-300 text-slate-900 placeholder:text-slate-500")
                            }
                        />
                    </div>
                </div>

                {error && (
                    <div
                        className={
                            "rounded-lg px-3 py-2 text-[11px] border " +
                            (isDark
                                ? "border-red-700 bg-red-950/60 text-red-100"
                                : "border-red-300 bg-red-50 text-red-700")
                        }
                    >
                        {error}
                    </div>
                )}

                <button
                    type="submit"
                    disabled={loading}
                    className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-red-600 hover:bg-red-700 disabled:bg-slate-500 text-xs md:text-sm font-medium text-white py-2.5 transition-colors shadow-md"
                >
                    {loading && (
                        <span className="w-4 h-4 border-2 border-white/60 border-t-transparent rounded-full animate-spin" />
                    )}
                    {loading ? "Analisando URL..." : "Analisar URL"}
                </button>

                <p
                    className={`text-[10px] leading-snug ${isDark ? "text-slate-400" : "text-slate-600"
                        }`}
                >
                    A interface envia a URL em um payload padronizado para a API
                    Java/Spring Boot, que aplica regras antifraude, consulta
                    whitelist/blacklist e, quando necessário, aciona a IA para
                    complementar o veredito.
                </p>
            </form>
        </div>
    );
};

export default VerificationForm;
