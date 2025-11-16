package com.antifraude.valores_receber_antifraude_api.aiAgent;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cliente de IA externa usando OpenAI de verdade.
 * Lê a API key do application.properties.
 */
@Component
public class ExternalAiClient {

    private final OpenAIClient client;
    private final ChatModel chatModel;

    public ExternalAiClient(
            @Value("${antifraude.external-ai.api-key}") String apiKey,
            @Value("${antifraude.external-ai.model:gpt-4o-mini}") String modelName) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("A chave antifraude.external-ai.api-key não está configurada.");
        }

        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();

        this.chatModel = mapModel(modelName);
    }

    private ChatModel mapModel(String modelName) {
        if (modelName == null) {
            return ChatModel.GPT_4O_MINI;
        }
        String m = modelName.toLowerCase();

        return switch (m) {
            case "gpt-4.1" -> ChatModel.GPT_4_1;
            case "gpt-4.1-mini" -> ChatModel.GPT_4_1_MINI;
            case "gpt-4o" -> ChatModel.GPT_4O;
            case "gpt-5.1" -> ChatModel.GPT_5_1;
            case "gpt-4o-mini" -> ChatModel.GPT_4O_MINI;
            default -> ChatModel.GPT_4O_MINI;
        };
    }

    /**
     * Chama a IA pedindo para classificar a URL e retornar um ExternalAiResponse
     * (Structured Outputs).
     */
    public ExternalAiResponse classify(
            String normalizedUrl,
            String domain,
            int rulesScoreBase,
            String evidenceSummary) {

        String systemPrompt = """
                Você é um classificador antifraude de URLs para bancos brasileiros.

                Tarefa:
                - Receber uma URL normalizada, domínio, score base de regras e um resumo de evidências técnicas.
                - Analisar risco de golpe (phishing / fraude financeira), considerando:
                  - Estrutura da URL e do domínio
                  - Padrões comuns de golpe (banco, governo, IR, FGTS, "valores a receber", etc.)
                  - Parecer técnico do sistema (evidenceSummary)
                - NÃO dependa de palavras exatas na URL. Considere também:
                  - Tamanho e complexidade incomum da URL
                  - Domínios estranhos tentando imitar domínios oficiais
                  - Mistura de termos de governo/banco com domínios genéricos
                  - Uso suspeito de subdomínios

                IMPORTANTE:
                - Você NÃO tem acesso à internet. Use somente os dados fornecidos no prompt.
                - Não tente "adivinhar" se é oficial ou não se não houver sinais claros.
                - Seja conservador: só marque como phishing quando tiver sinais fortes.

                Formato de saída (JSON estruturado):
                - riskScore: número entre 0.0 e 1.0 indicando o risco de golpe.
                - phishing: booleano (true/false).
                - explanation: texto curto explicando o porquê da decisão.
                """;

        String userPrompt = String.format(
                """
                        Analise a seguinte URL para possível golpe:

                        URL normalizada: %s
                        Domínio: %s
                        Score base das regras locais: %d
                        Evidências técnicas do sistema: %s

                        Avalie o risco de ser um golpe de "valores a receber" / golpe financeiro
                        e preencha os campos do JSON (riskScore, phishing, explanation).
                        """,
                normalizedUrl,
                domain,
                rulesScoreBase,
                evidenceSummary);

        try {
            StructuredChatCompletionCreateParams<ExternalAiResponse> params = StructuredChatCompletionCreateParams
                    .<ExternalAiResponse>builder()
                    .model(this.chatModel)
                    .addSystemMessage(systemPrompt)
                    .addUserMessage(userPrompt)
                    .responseFormat(ExternalAiResponse.class)
                    .build();

            StructuredChatCompletion<ExternalAiResponse> completion = client.chat().completions().create(params);

            // Pega o primeiro ExternalAiResponse retornado pela mensagem
            ExternalAiResponse parsed = completion
                    .choices()
                    .stream()
                    .flatMap(choice -> choice.message().content().stream())
                    .findFirst()
                    .orElse(null);

            return parsed;
        } catch (Exception e) {
            System.err.println("Erro na IA externa (OpenAI): " + e.getMessage());
            e.printStackTrace();

            ExternalAiResponse fallback = new ExternalAiResponse();
            fallback.setRiskScore(0.5);
            fallback.setPhishing(false);
            fallback.setExplanation("Erro ao chamar a OpenAI: " + e.getMessage());
            return fallback;
        }
    }
}
