package com.agentdesk.agent.tools;

/**
 * 递归下降表达式求值器：支持 + - * / % ^ 与括号，不使用脚本引擎，避免注入风险。
 */
public final class ExpressionEvaluator {

    private ExpressionEvaluator() {
    }

    public static double evaluate(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("表达式不能为空");
        }
        Parser parser = new Parser(expression);
        double value = parser.parseExpression();
        parser.skipSpaces();
        if (parser.pos < parser.text.length()) {
            throw new IllegalArgumentException("表达式包含无法识别的字符：" + parser.text.charAt(parser.pos));
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("计算结果不是有效数字");
        }
        return value;
    }

    private static final class Parser {

        private final String text;
        private int pos;

        private Parser(String text) {
            this.text = text;
        }

        private double parseExpression() {
            double value = parseTerm();
            while (true) {
                skipSpaces();
                if (match('+')) {
                    value += parseTerm();
                } else if (match('-')) {
                    value -= parseTerm();
                } else {
                    return value;
                }
            }
        }

        private double parseTerm() {
            double value = parseUnary();
            while (true) {
                skipSpaces();
                if (match('*')) {
                    value *= parseUnary();
                } else if (match('/')) {
                    double divisor = parseUnary();
                    if (divisor == 0) {
                        throw new IllegalArgumentException("除数不能为 0");
                    }
                    value /= divisor;
                } else if (match('%')) {
                    double divisor = parseUnary();
                    if (divisor == 0) {
                        throw new IllegalArgumentException("取模的除数不能为 0");
                    }
                    value %= divisor;
                } else {
                    return value;
                }
            }
        }

        private double parseUnary() {
            skipSpaces();
            if (match('-')) {
                return -parseUnary();
            }
            if (match('+')) {
                return parseUnary();
            }
            return parsePower();
        }

        private double parsePower() {
            double base = parsePrimary();
            skipSpaces();
            if (match('^')) {
                return Math.pow(base, parseUnary());
            }
            return base;
        }

        private double parsePrimary() {
            skipSpaces();
            if (match('(')) {
                double value = parseExpression();
                skipSpaces();
                if (!match(')')) {
                    throw new IllegalArgumentException("括号不匹配");
                }
                return value;
            }
            return parseNumber();
        }

        private double parseNumber() {
            skipSpaces();
            int start = pos;
            while (pos < text.length() && (Character.isDigit(text.charAt(pos)) || text.charAt(pos) == '.')) {
                pos++;
            }
            if (start == pos) {
                throw new IllegalArgumentException("表达式不合法，位置 " + pos);
            }
            return Double.parseDouble(text.substring(start, pos));
        }

        private boolean match(char expected) {
            if (pos < text.length() && text.charAt(pos) == expected) {
                pos++;
                return true;
            }
            return false;
        }

        private void skipSpaces() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
        }
    }
}
