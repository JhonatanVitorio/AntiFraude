import React, { useState } from "react";
import { API_BASE_URL } from "./config";
import { CheckResponse, Tab } from "./types";
import { useTheme } from "./hooks/useTheme";

import Shell from "./components/layout/Shell";
import VerificationForm from "./components/verification/VerificationForm";
import VerificationResult from "./components/verification/VerificationResult";
import RiskDashboard from "./components/dashboard/RiskDashboard";
import HistoryTable from "./components/history/HistoryTable";
import RulesPanel from "./components/rules/RulesPanel";
import ArchitecturePanel from "./components/architecture/ArchitecturePanel";

const API_URL = `${API_BASE_URL}/api/v1/checks`;

const App: React.FC = () => {
  const { theme, toggleTheme } = useTheme("dark");
  const [activeTab, setActiveTab] = useState<Tab>("verificacao");

  const [url, setUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<CheckResponse | null>(null);
  const [history, setHistory] = useState<CheckResponse[]>([]);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    setResult(null);

    const normalized = url.trim();

    if (!normalized) {
      setError("Por favor, insira uma URL para análise.");
      return;
    }

    if (!/^https?:\/\//i.test(normalized)) {
      setError("A URL deve começar com http:// ou https://");
      return;
    }

    try {
      setLoading(true);

      const response = await fetch(API_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          inputType: "URL",
          rawInput: normalized,
          metadata: {},
        }),
      });

      if (!response.ok) {
        throw new Error(`Erro ao consultar a API (${response.status})`);
      }

      const data: CheckResponse = await response.json();

      const enriched: CheckResponse = {
        ...data,
        rawInput: normalized,
        submittedAt: data.submittedAt || new Date().toISOString(),
      };

      setResult(enriched);
      setHistory((prev) => [enriched, ...prev].slice(0, 8));
    } catch (err) {
      console.error(err);
      setError(
        "Não foi possível concluir a análise no momento. Tente novamente em instantes."
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <Shell
      theme={theme}
      onToggleTheme={toggleTheme}
      activeTab={activeTab}
      onChangeTab={setActiveTab}
    >
      {activeTab === "verificacao" && (
        <div className="grid gap-6 md:grid-cols-2 items-start">
          <VerificationForm
            theme={theme}
            url={url}
            error={error}
            loading={loading}
            onUrlChange={setUrl}
            onSubmit={handleSubmit}
          />
          <VerificationResult theme={theme} loading={loading} result={result} />
        </div>
      )}

      {activeTab === "dashboard" && (
        <RiskDashboard theme={theme} history={history} />
      )}

      {activeTab === "historico" && (
        <HistoryTable theme={theme} history={history} />
      )}

      {activeTab === "regras" && <RulesPanel theme={theme} />}

      {activeTab === "arquitetura" && (
        <ArchitecturePanel theme={theme} />
      )}
    </Shell>
  );
};

export default App;
