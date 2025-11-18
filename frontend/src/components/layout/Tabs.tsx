import React from "react";
import type { Tab, Theme } from "../../types";

interface TabsProps {
    theme: Theme;
    activeTab: Tab;
    onChangeTab: (tab: Tab) => void;
}

const Tabs: React.FC<TabsProps> = ({ theme, activeTab, onChangeTab }) => {
    const isDark = theme === "dark";

    const tabs: [Tab, string][] = [
        ["verificacao", "Verificação em tempo real"],
        ["dashboard", "Dashboard"],
        ["historico", "Histórico de verificações"],
        ["regras", "Regras & alertas"],
        ["arquitetura", "Arquitetura / IA"],
    ];

    return (
        <div className="flex flex-wrap gap-2 mb-6 text-[11px] md:text-xs">
            {tabs.map(([tab, label]) => {
                const isActive = activeTab === tab;
                return (
                    <button
                        key={tab}
                        type="button"
                        onClick={() => onChangeTab(tab)}
                        className={`px-3 py-1.5 rounded-full border transition-all ${isActive
                                ? "bg-red-600 text-white border-red-500 shadow-md"
                                : isDark
                                    ? "bg-slate-900/60 border-slate-700 text-slate-300 hover:bg-slate-800"
                                    : "bg-white border-slate-300 text-slate-700 hover:bg-slate-100"
                            }`}
                    >
                        {label}
                    </button>
                );
            })}
        </div>
    );
};

export default Tabs;
