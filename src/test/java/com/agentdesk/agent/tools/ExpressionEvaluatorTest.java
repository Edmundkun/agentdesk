package com.agentdesk.agent.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpressionEvaluatorTest {

    @Test
    @DisplayName("运算符优先级：先乘除后加减")
    void shouldRespectPrecedence() {
        assertThat(ExpressionEvaluator.evaluate("1+2*3")).isEqualTo(7);
        assertThat(ExpressionEvaluator.evaluate("10-6/2")).isEqualTo(7);
    }

    @Test
    @DisplayName("支持括号与幂运算")
    void shouldSupportParenthesesAndPower() {
        assertThat(ExpressionEvaluator.evaluate("(1+2)*3")).isEqualTo(9);
        assertThat(ExpressionEvaluator.evaluate("2^3^2")).isEqualTo(512);
        assertThat(ExpressionEvaluator.evaluate("(120-15)*0.8")).isCloseTo(84.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    @DisplayName("支持一元负号与取模")
    void shouldSupportUnaryAndModulo() {
        assertThat(ExpressionEvaluator.evaluate("-3+5")).isEqualTo(2);
        assertThat(ExpressionEvaluator.evaluate("10%3")).isEqualTo(1);
    }

    @Test
    @DisplayName("除数为 0 抛出可读异常")
    void shouldRejectDivisionByZero() {
        assertThatThrownBy(() -> ExpressionEvaluator.evaluate("1/0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("除数不能为 0");
    }

    @Test
    @DisplayName("非法表达式抛出异常")
    void shouldRejectInvalidExpression() {
        assertThatThrownBy(() -> ExpressionEvaluator.evaluate("1+")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExpressionEvaluator.evaluate("(1+2")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExpressionEvaluator.evaluate("1+a")).isInstanceOf(IllegalArgumentException.class);
    }
}
