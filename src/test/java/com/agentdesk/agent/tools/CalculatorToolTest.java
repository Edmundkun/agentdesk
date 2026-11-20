package com.agentdesk.agent.tools;

import com.agentdesk.agent.AgentContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculatorToolTest {

    private final CalculatorTool tool = new CalculatorTool();

    @Test
    @DisplayName("计算四则运算并返回整数结果")
    void shouldCalculateIntegerResult() {
        assertThat(tool.execute("128*3+45", context())).isEqualTo("429");
    }

    @Test
    @DisplayName("兼容中文括号与乘除号，小数结果保留有效位")
    void shouldNormalizeChineseSymbols() {
        assertThat(tool.execute("（120-15）×0.8", context())).isEqualTo("84");
        assertThat(tool.execute("7÷2", context())).isEqualTo("3.5");
    }

    @Test
    @DisplayName("包含非法字符时拒绝执行，防止注入")
    void shouldRejectIllegalCharacters() {
        assertThatThrownBy(() -> tool.execute("Runtime.getRuntime()", context()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("仅支持数字");
    }

    private AgentContext context() {
        return new AgentContext(1L, 1L);
    }
}
