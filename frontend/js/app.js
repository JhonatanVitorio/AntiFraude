// URL da API do backend (Spring Boot)
const API_URL = "http://localhost:8080/api/v1/checks";

const urlInput = document.getElementById("url-input");
const buscarBtn = document.getElementById("buscar-btn");
const errorMessage = document.getElementById("error-message");

const resultCard = document.getElementById("result-card");
const resultTitle = document.getElementById("result-title");
const resultDescription = document.getElementById("result-description");
const resultScore = document.getElementById("result-score");

const resultUrl = document.getElementById("result-url");
const resultDomain = document.getElementById("result-domain");
const resultSource = document.getElementById("result-source");
const resultSubmitted = document.getElementById("result-submitted");

const resultReasonsTitle = document.getElementById("result-reasons-title");
const resultReasons = document.getElementById("result-reasons");

function setLoading(isLoading) {
    if (isLoading) {
        buscarBtn.textContent = "Verificando...";
        buscarBtn.disabled = true;
    } else {
        buscarBtn.textContent = "Buscar";
        buscarBtn.disabled = false;
    }
}

function showError(message) {
    if (!message) {
        errorMessage.classList.add("hidden");
        errorMessage.textContent = "";
        return;
    }

    errorMessage.textContent = message;
    errorMessage.classList.remove("hidden");
}

function clearResult() {
    resultCard.className = "result-card hidden"; // reseta classes
    resultTitle.textContent = "";
    resultDescription.textContent = "";
    resultScore.textContent = "";

    resultUrl.textContent = "";
    resultDomain.textContent = "";
    resultSource.textContent = "";
    resultSubmitted.textContent = "";

    resultReasons.innerHTML = "";
    resultReasonsTitle.classList.add("hidden");
}

function formatDate(isoString) {
    if (!isoString) return "";
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return "";
    return date.toLocaleString("pt-BR");
}

function renderResult(data) {
    clearResult();

    // 👉 adapta ao JSON REAL da API
    // {
    //   "id": "...",
    //   "verdict": "SUSPECT",
    //   "score": 0,
    //   "ruleHits": ["string"],
    //   "evidenceSummary": ["string"],
    //   "normalizedUrl": "string",
    //   "domain": "string",
    //   "source": "string",
    //   "submittedAt": "2025-11-15T20:35:01.451Z"
    // }

    const verdictRaw = (data.verdict || "").toString().toUpperCase();
    let score = data.score;

    const ruleHits = Array.isArray(data.ruleHits) ? data.ruleHits : [];
    const evidenceSummary = Array.isArray(data.evidenceSummary) ? data.evidenceSummary : [];

    // Junta regras + evidências em uma lista só
    const reasons = [
        ...ruleHits.map(r => `Regra acionada: ${r}`),
        ...evidenceSummary.map(e => `Evidência: ${e}`)
    ];

    let statusClass = "";
    let titleText = "";
    let descriptionText = "";

    if (verdictRaw === "SAFE" || verdictRaw === "SEGURO") {
        statusClass = "result-safe";
        titleText = "URL segura ✅";
        descriptionText = "Não encontramos indícios fortes de fraude neste endereço.";
    } else if (verdictRaw === "SUSPECT" || verdictRaw === "SUSPICIOUS" || verdictRaw === "SUSPEITO") {
        statusClass = "result-suspicious";
        titleText = "URL suspeita ⚠️";
        descriptionText = "Identificamos sinais de risco. Recomendamos NÃO acessar nem informar dados pessoais.";
    } else if (verdictRaw === "FRAUD" || verdictRaw === "MALICIOUS" || verdictRaw === "GOLPE") {
        statusClass = "result-fraud";
        titleText = "Possível golpe 🚨";
        descriptionText = "Nossa análise indica alta probabilidade de fraude. NÃO clique nem informe dados.";
    } else {
        statusClass = "result-suspicious";
        titleText = "Não foi possível classificar a URL 🤔";
        descriptionText = "A análise não retornou um veredito claro. Tenha cautela.";
    }

    resultCard.classList.remove("hidden");
    resultCard.classList.add(statusClass);
    resultTitle.textContent = titleText;
    resultDescription.textContent = descriptionText;

    // Score: trata tanto 0–100 quanto 0–1
    if (typeof score === "number") {
        let normalized = score;
        if (score >= 0 && score <= 1) {
            normalized = Math.round(score * 100);
        }
        resultScore.textContent = "Score de risco: " + normalized + " / 100";
    } else {
        resultScore.textContent = "";
    }

    // Metadados
    if (data.normalizedUrl) {
        resultUrl.textContent = "URL normalizada: " + data.normalizedUrl;
    }
    if (data.domain) {
        resultDomain.textContent = "Domínio analisado: " + data.domain;
    }
    if (data.source) {
        resultSource.textContent = "Origem da análise: " + data.source;
    }
    if (data.submittedAt) {
        resultSubmitted.textContent = "Data da submissão: " + formatDate(data.submittedAt);
    }

    // Lista de motivos
    if (reasons.length > 0) {
        resultReasonsTitle.classList.remove("hidden");
        reasons.forEach((reason) => {
            const li = document.createElement("li");
            li.textContent = reason;
            resultReasons.appendChild(li);
        });
    }
}

async function handleBuscar() {
    const url = urlInput.value.trim();
    showError("");
    clearResult();

    if (!url) {
        showError("Por favor, insira uma URL para verificar.");
        return;
    }

    if (!/^https?:\/\//i.test(url)) {
        showError("A URL deve começar com http:// ou https://");
        return;
    }

    try {
        setLoading(true);

        const response = await fetch(API_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            // backend espera { "url": "..." }
            body: JSON.stringify({
                inputType: "URL",
                rawInput: url,
                metadata: {}
            })
        });

        if (!response.ok) {
            throw new Error("Erro ao consultar a API (" + response.status + ")");
        }

        const data = await response.json();
        renderResult(data);
    } catch (error) {
        console.error(error);
        showError("Não foi possível verificar a URL. Tente novamente em instantes.");
    } finally {
        setLoading(false);
    }
}

buscarBtn.addEventListener("click", handleBuscar);

// Permitir enviar com Enter
urlInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
        handleBuscar();
    }
});
