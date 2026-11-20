package com.agentdesk;

import com.agentdesk.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AgentDesk 启动类。
 *
 * <p>企业知识库智能体工作台：文档入库 → 切片向量化 → 混合检索 → ReAct Agent 多轮推理 → SSE 流式输出。</p>
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties(AppProperties.class)
public class AgentDeskApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentDeskApplication.class, args);
    }

    /**
     * 对话推理线程池：SSE 长连接不占用 Tomcat 请求线程，避免慢请求把容器线程打满。
     */
    @Bean(name = "agentExecutorPool", destroyMethod = "shutdown")
    public ExecutorService agentExecutorPool() {
        AtomicInteger seq = new AtomicInteger();
        return Executors.newFixedThreadPool(8, runnable -> {
            Thread thread = new Thread(runnable, "agent-worker-" + seq.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }
}
