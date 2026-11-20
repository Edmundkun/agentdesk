package com.agentdesk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 项目自定义配置，统一以 {@code agentdesk.*} 为前缀，见 application.yml。
 */
@Data
@ConfigurationProperties(prefix = "agentdesk")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Llm llm = new Llm();
    private Agent agent = new Agent();
    private Rag rag = new Rag();
    private Demo demo = new Demo();

    @Data
    public static class Jwt {
        private String secret = "agentdesk-local-dev-secret-key-change-me-please-2026";
        private int expireHours = 168;
    }

    @Data
    public static class Llm {
        /** mock：内置规则模型；openai：OpenAI 兼容接口 */
        private String mode = "mock";
        private String baseUrl = "https://api.deepseek.com/v1";
        private String apiKey = "";
        private String model = "deepseek-chat";
        private int timeoutSeconds = 60;
        private double temperature = 0.2;
    }

    @Data
    public static class Agent {
        /** ReAct 最大推理轮数 */
        private int maxSteps = 5;
        /** 带多少条历史消息进上下文 */
        private int historySize = 6;
    }

    @Data
    public static class Rag {
        private int chunkSize = 420;
        private int chunkOverlap = 80;
        private int topK = 4;
        private double minScore = 0.08;
        /** 混合检索中关键词得分权重（其余为向量余弦） */
        private double keywordWeight = 0.3;
    }

    @Data
    public static class Demo {
        private boolean enabled = true;
    }
}
