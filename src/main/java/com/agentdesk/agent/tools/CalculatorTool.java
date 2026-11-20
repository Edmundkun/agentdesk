package com.agentdesk.agent.tools;

import com.agentdesk.agent.AgentContext;
import com.agentdesk.agent.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 计算器工具：四则运算、取模、幂运算，纯 Java 实现。
 */
@Component
public class CalculatorTool implements Tool {

    @Override
    public String name() {
        return "calculator";
    }

    @Override
    public String description() {
        return "计算数学表达式，支持 + - * / % ^ 和括号，例如 128*3+45、(120-15)*0.8。输入为纯表达式。";
    }

    @Override
    public String execute(String input, AgentContext context) {
        String expression = normalize(input);
        if (expression.isBlank()) {
            throw new IllegalArgumentException("表达式不能为空");
        }
        if (!expression.matches("[0-9+\\-*/%^().\\s]+")) {
            throw new IllegalArgumentException("仅支持数字与 + - * / % ^ ( ) 运算符");
        }
        double value = ExpressionEvaluator.evaluate(expression);
        return format(value);
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("×", "*")
                .replace("÷", "/")
                .replace("（", "(")
                .replace("）", ")")
                .replace("=", "")
                .replaceAll("\\s+", "")
                .trim();
    }

    private String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 9.0e15) {
            return String.valueOf((long) value);
        }
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
