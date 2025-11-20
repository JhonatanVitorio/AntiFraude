<div align="center">

# 🔐 Valores a Receber – Antifraude API

### Sistema Inteligente de Detecção de Golpes

Java 21 • Spring Boot 3 • IA • Threat Intel • Rules Engine • Testes

---

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen)
![JUnit](https://img.shields.io/badge/JUnit-5-blue)
![Status](https://img.shields.io/badge/Build-Passing-success)
![Coverage](https://img.shields.io/badge/Tests-Unitários%20%2B%20Integração-green)

</div>

---

# 📘 1. Visão Geral

Esta API analisa URLs suspeitas para identificar possíveis golpes relacionados a **“Valores a Receber”** utilizando:

- 🔹 *Rules Engine*
- 🔹 IA Api OpenAi
- 🔹 *Threat Intelligence*
- 🔹 Persistência automática de **Whitelist**, **Blacklist** e **Histórico**
- 🔹 Testes unitários e de integração (MockMvc + Repositórios + Serviços)

---

# 🧠 2. Arquitetura do Projeto

```text
src/
└── main/
    └── java/
        └── com/antifraude/valores_receber_antifraude_api/
            ├── features/
            │   └── check/
            │       ├── controller/       # CheckController (endpoints REST)
            │       |── service/          # CheckService (pipeline antifraude)
            |       └── dto/              # CheckRequest, CheckResponse, InputType
            │
            ├── core/
            │   ├── rules/                # RulesEngine + UrlNormalizer
            │   |── threatintel/          # ThreatIntelService (reputação/heurísticas)
            |   ├── repository/           # Spring Data JPA Repositories
            |   ├── rules/                # Motor de regras e UrlNormalizer
            |   └── model/
            │       ├── entity/           # Entidades JPA (UrlRecord, BlacklistEntry, WhitelistEntry)
            │       └── enums/            # Enums (Verdict, ListEntryType, etc.)
            │
            ├── aiAgent/                  # AiAgentService + cliente de IA externa
            │
            ├── config/                   # RestTemplateConfig
            │
            ├── lists/
            │   |── service/              # ListsService (whitelist / blacklist)
            │   |── dto/                  # ListEntry (CreateRequest / Response)
            |   └── controller/           # ListController (whitelistController / blacklistController) 
            |
            ├── util/                     # Utilitários (se houver)
            │
            └── ValoresReceberAntifraudeApiApplication.java  # Classe principal Spring Boot

test/
└── java/
    └── com/antifraude/valores_receber_antifraude_api/
        ├── aiAgent/                      # Tests do AiAgentService
        ├── core/
        │   ├── rules/                    # RulesEngineTest
        │   └── threatintel/              # ThreatIntelServiceImplTest
        ├── features/
        │   └── check/
        │       ├── api/                  # CheckApiIntegrationTest (MockMvc)
        │       └── service/              # CheckServiceIntegrationTest
        └── ValoresReceberAntifraudeApiApplicationTests.java  # Teste de contexto

```

---

# 🔍 3. Pipeline de Verificação

Fluxo completo aplicado a toda requisição:

1. Normalização da URL
2. Checagem Whitelist
3. Checagem Blacklist
4. Cache (historico)
5. Rules Engine
6. Threat Intelligence
7. IA (phishing heuristics)
8. Persistência (UrlRecord + listas)
9. Resposta Final

---

# 📡 4. Endpoint Principal

## ▶️ POST `/api/v1/checks`

### 🔸 Requisição:

```json
{
  "rawInput": "http://exemplo-site.com",
  "inputType": "URL"
}

{
  "id": "d1b1c0a4-4d1a-4c76-9eaf-33ab9d2f0911",
  "verdict": "SUSPECT",
  "score": 88,
  "ruleHits": ["DOMAIN_SUSPICIOUS", "HTTP_NO_TLS"],
  "evidenceSummary": [
    "Domínio contém padrões fraudulentos",
    "URL usa HTTP sem TLS"
  ],
  "normalizedUrl": "http://exemplo-site.com",
  "domain": "exemplo-site.com",
  "source": "COMBINED",
  "submittedAt": "2025-11-14T16:00:00"
}
```

# 📘 5. Regras Aplicadas (Rules Engine)

  ❌ Suspeitas de phishing:

- Domínio contém "secure", "banking", "confirmacao", "verificador"
- Domínios falsos de governo e bancos
- Falsos encurtadores como:
- bit-llly
- tinyurl-security-check
- secure-auth-xyz

⚠️ HTTP sem TLS:

- URL iniciando com http:// → +25 score

⚠️ Score máximo de regras → 100

- Acima de 60 → SUSPECT

# 🧠 6. IA – Classificação Inteligente

O módulo IA combina heurísticas que simulam:

- análise semântica de phishing
- reputação
- padrão do domínio
- falsificação de marca (spoofing)

Retornos possíveis:

- IA_CLEAN
- IA_PHISHING
- IA_INCONCLUSIVE
- IA_ERROR

# 🗄️ 7. Persistência Automática

Tabela	              Descrição

- url_record	        - Histórico de verificações
- blacklist_entry	    - URLs suspeitas detectadas
- whitelist_entry	    - URLs confiáveis

🔒 Regras de persistência:

- LEGIT → Whitelist
- SUSPECT → Blacklist
- Sempre → UrlRecord

# 🧪 8. Testes

O projeto possui testes profissionais, incluindo:

✔️ Unitários

- RulesEngineTest
- ThreatIntelServiceImplTest
- AiAgentServiceTest

✔️ Integração

- CheckServiceIntegrationTest
- CheckApiIntegrationTest (MockMvc)

✔️ Teste de contexto Spring Boot

- ValoresReceberAntifraudeApiApplicationTests

▶️ Rodar testes:
  ---- mvn clean test ----

# 🚀 9. Como Rodar Localmente

▶️ Clonar:
  git clone https://github.com/seu-repo.git - cd valores-receber-antifraude-api

▶️ Rodar:
  mvn spring-boot:run

▶️ Swagger:
  http://localhost:8080/swagger-ui/index.html#/

# 🔮 10. Melhorias Futuras

- Integração real com VirusTotal / Google Safe Browsing
- Aprendizado de máquina real
- Webhook para notificar golpes automaticamente
- Integração com ElasticSearch para logs e auditoria
