package com.agentdesk.agent;

import com.agentdesk.config.AppProperties;
import com.agentdesk.llm.ChatModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class AgentExecutorTest {

    private static final Tool ECHO_TOOL = new Tool() {
        @Override
        public String name() {
            return "knowledge_search";
        }

        @Override
        public String description() {
            return "检索知识库";
        }

        @Override
        public String execute(String input, AgentContext context) {
            return "检索结果：" + input;
        }
    };

    private AgentExecutor executorWith(String... outputs) {
        Queue<String> queue = new ArrayDeque<>(List.of(outputs));
        ChatModel model = new ChatModel() {
            @Override
            public String chat(String prompt) {
                return queue.isEmpty() ? null : queue.poll();
            }

            @Override
            public String name() {
                return "stub-model";
            }
        };
        AppProperties properties = new AppProperties();
        properties.getAgent().setMaxSteps(5);
        return new AgentExecutor(model, new ToolRegistry(List.of(ECHO_TOOL)), properties);
    }

    @Test
    @DisplayName("先调用工具，再根据 Observation 输出 Final Answer")
    void shouldCallToolThenAnswer() {
        AgentExecutor executor = executorWith(
                "Thought: 需要检索知识库\nAction: knowledge_search\nAction Input: 报销时限",
                "Thought: 信息足够\nFinal Answer: 差旅报销需在 10 个工作日内提交。");

        AgentResult result = executor.execute(new AgentContext(1L, 1L), "报销时限是多久？", List.of());

        assertThat(result.answer()).isEqualTo("差旅报销需在 10 个工作日内提交。");
        assertThat(result.steps()).hasSize(1);
        AgentStep step = result.steps().get(0);
        assertThat(step.getTool()).isEqualTo("knowledge_search");
        assertThat(step.getInput()).isEqualTo("报销时限");
        assertThat(step.getObservation()).contains("检索结果：报销时限");
        assertThat(step.getThought()).isEqualTo("需要检索知识库");
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("模型直接给出 Final Answer 时不记录工具步骤")
    void shouldReturnDirectAnswer() {
        AgentResult result = executorWith("Thought: 直接回答\nFinal Answer: 你好，我是 AgentDesk。")
                .execute(new AgentContext(1L, 1L), "你好", List.of());

        assertThat(result.answer()).isEqualTo("你好，我是 AgentDesk。");
        assertThat(result.steps()).isEmpty();
    }

    @Test
    @DisplayName("未知工具不中断对话，将异常转成 Observation")
    void shouldConvertToolFailureToObservation() {
        AgentExecutor executor = executorWith(
                "Thought: 试试不存在的工具\nAction: unknown_tool\nAction Input: x",
                "Thought: 工具失败\nFinal Answer: 工具不可用，我直接回答。");

        AgentResult result = executor.execute(new AgentContext(1L, 1L), "测试", List.of());

        assertThat(result.steps()).hasSize(1);
        assertThat(result.steps().get(0).getObservation()).contains("工具执行失败");
        assertThat(result.answer()).contains("工具不可用");
    }

    @Test
    @DisplayName("重复调用同一工具时提前终止，避免死循环")
    void shouldStopRepeatedToolCalls() {
        AgentExecutor executor = executorWith(
                "Thought: 第一次\nAction: knowledge_search\nAction Input: 报销",
                "Thought: 第二次\nAction: knowledge_search\nAction Input: 报销",
                "Thought: 第三次\nAction: knowledge_search\nAction Input: 报销");

        AgentResult result = executor.execute(new AgentContext(1L, 1L), "循环测试", List.of());

        assertThat(result.steps()).hasSize(2);
        assertThat(result.answer()).contains("2 轮推理");
    }

    @Test
    @DisplayName("模型输出为空时返回兜底文案")
    void shouldFallbackWhenModelReturnsNothing() {
        AgentResult result = executorWith().execute(new AgentContext(1L, 1L), "空响应", List.of());

        assertThat(result.answer()).contains("抱歉");
        assertThat(result.steps()).isEmpty();
    }
}
