package com.agentdesk.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockChatModelTest {

    private final MockChatModel model = new MockChatModel();

    private String prompt(String question, String scratchpad) {
        return "【历史对话】\n（暂无）\n\n【用户问题】\n" + question + "\n\n【已有推理】\n" + scratchpad;
    }

    @Test
    @DisplayName("知识类问题先调用 knowledge_search")
    void shouldCallKnowledgeSearch() {
        String output = model.chat(prompt("差旅报销多久内提交？", "（暂无）"));

        assertThat(output).contains("Action: knowledge_search");
        assertThat(output).contains("Action Input: 差旅报销多久内提交？");
    }

    @Test
    @DisplayName("计算类问题调用 calculator 并提取表达式")
    void shouldCallCalculator() {
        String output = model.chat(prompt("帮我算一下 128*3+45 等于多少", "（暂无）"));

        assertThat(output).contains("Action: calculator");
        assertThat(output).contains("Action Input: 128*3+45");
    }

    @Test
    @DisplayName("时间类问题调用 current_time")
    void shouldCallCurrentTime() {
        String output = model.chat(prompt("现在几点了", "（暂无）"));

        assertThat(output).contains("Action: current_time");
    }

    @Test
    @DisplayName("拿到 Observation 后输出 Final Answer")
    void shouldAnswerAfterObservation() {
        String scratchpad = "Thought: 需要检索\nAction: knowledge_search\nAction Input: 报销时限\n"
                + "Observation: [1] 《员工报销制度》第 2 段：需在 10 个工作日内提交。";

        String output = model.chat(prompt("报销多久内提交", scratchpad));

        assertThat(output).contains("Final Answer:");
        assertThat(output).contains("10 个工作日内提交");
    }

    @Test
    @DisplayName("空问题给出引导文案而不是报错")
    void shouldGuideWhenQuestionBlank() {
        assertThat(model.chat(prompt("", "（暂无）"))).contains("Final Answer");
    }
}
