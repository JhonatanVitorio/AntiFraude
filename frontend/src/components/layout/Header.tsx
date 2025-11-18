import React from "react";
import type { Theme } from "../../types";

interface HeaderProps {
    theme: Theme;
    onToggleTheme: () => void;
}

const Header: React.FC<HeaderProps> = ({ theme, onToggleTheme }) => {
    const isDark = theme === "dark";

    return (
        <div className="flex items-center justify-between gap-4 mb-6">
            <div className="flex items-center gap-3">
                <div className="relative">
                    <div className="w-11 h-11 rounded-2xl bg-gradient-to-br from-red-500 via-red-600 to-red-700 flex items-center justify-center text-white font-bold text-2xl shadow-lg animate-pulse">
                        B
                    </div>
                    <div className="absolute -inset-[2px] rounded-3xl border border-red-500/40 opacity-60" />
                </div>
                <div>
                    <p
                        className={`text-sm font-semibold tracking-tight ${isDark ? "text-slate-50" : "text-slate-900"
                            }`}
                    >
                        VerificaBC
                    </p>
                    <p
                        className={`text-[11px] ${isDark ? "text-slate-400" : "text-slate-500"
                            }`}
                    >
                        Módulo antifraude · Golpe &quot;valores a receber&quot;
                    </p>
                </div>
            </div>

            <button
                type="button"
                onClick={onToggleTheme}
                className={`flex items-center gap-2 text-xs px-3 py-1.5 rounded-full border transition-colors ${isDark
                        ? "border-slate-600 bg-slate-900/80 hover:bg-slate-800 text-slate-100"
                        : "border-slate-300 bg-white hover:bg-slate-100 text-slate-800"
                    }`}
            >
                <span className="text-[11px]">
                    Tema: <strong>{isDark ? "Dark" : "Light"}</strong>
                </span>
                <span>{isDark ? "🌙" : "☀️"}</span>
            </button>
        </div>
    );
};

export default Header;
