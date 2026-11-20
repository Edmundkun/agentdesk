package com.agentdesk.llm;

import com.agentdesk.common.BizException;
import com.agentdesk.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议客户端：DeepSeek / 通义千问 / OpenAI 均可直接使用。
 *
 * <p>配置示例：</p>
 * <pre>
 * LLM_MODE=openai
 * LLM_BASE_URL=https://api.deepseek.com/v1
 * LLM_API_KEY=sk-xxx
 * LLM_MODEL=deepseek-chat
 * </pre>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "agentdesk.llm", name = "mode", havingValue = "openai")
public class OpenAiChatModel implements ChatModel {

    private final AppProperties.Llm config;
    private final RestClient restClient;

    public OpenAiChatModel(AppProperties properties) {
        this.config = properties.getLlm();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(config.getTimeoutSeconds()));
        this.restClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public String chat(String prompt) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new BizException(500, "当前为 openai 模式，但未配置 LLM_API_KEY");
        }
        Map<String, Object> body = Map.of(
                "model", config.getModel(),
                "temperature", config.getTemperature(),
                "stream", false,
                "messages", List.of(Map.of("role", "user", "content", prompt)));

        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw new BizException(502, "大模型返回为空");
            }
            return response.path("choices").path(0).path("message").path("content").asText("");
        } catch (RestClientResponseException e) {
            log.error("调用大模型失败 status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BizException(502, "调用大模型失败：" + e.getStatusCode() + " " + e.getResponseBodyAsString());
        }
    }

    @Override
    public String name() {
        return config.getModel();
    }
}
