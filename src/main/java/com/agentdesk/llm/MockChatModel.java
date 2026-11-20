package com.agentdesk.llm;

import com.agentdesk.agent.AgentPrompts;
import com.agentdesk.embedding.TextTokenizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 内置规则模型：不依赖任何 API Key，也能完整跑通 ReAct 的「思考 → 调工具 → 观察 → 回答」闭环。
 *
 * <p>它不是“假装调用大模型”，而是一个可解释的决策器：按问题类型选择工具、解析工具观察结果并组织答案，
 * 用于本地开发、CI 与演示环境。切换到 {@code agentdesk.llm.mode=openai} 即可由真实大模型驱动，
 * 上层 Agent 执行器代码完全不用改。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "agentdesk.llm", name = "mode", havingValue = "mock", matchIfMissing = true)
public class MockChatModel implements ChatModel {

    private static final Pattern GREETING = Pattern.compile("^(你好|您好|hi|hello|嗨|在吗|哈喽)[!！。~\\s]*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_INTENT = Pattern.compile("(几点|现在时间|当前时间|今天几号|今天日期|星期几|日期是)");
    private static final Pattern STATS_INTENT = Pattern.compile("(文档|知识库|资料|片段).{0,6}(统计|多少|数量)|多少.{0,4}(文档|片段)");
    private static final Pattern MATH_CHARS = Pattern.compile("^[0-9+\\-*/%^().\\s]+$");
    private static final Pattern HAS_OPERATOR = Pattern.compile("[+\\-*/%^]");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");

    @Override
    public String chat(String prompt) {
        String question = section(prompt, AgentPrompts.QUESTION_HEADER, AgentPrompts.SCRATCHPAD_HEADER);
        String scratchpad = section(prompt, AgentPrompts.SCRATCHPAD_HEADER, null);

        if (scratchpad != null && !scratchpad.isBlank() && !scratchpad.startsWith("（")) {
            return finalAnswer(scratchpad, question == null ? "" : question.trim());
        }
        return nextAction(question == null ? "" : question.trim());
    }

    private String nextAction(String question) {
        if (question.isBlank()) {
            return "Thought: 用户没有提供有效问题。\nFinal Answer: 请先描述你想了解的内容，例如“差旅报销需要哪些材料？”。";
        }
        if (GREETING.matcher(question).matches()) {
            return "Thought: 这是问候语，无需调用工具。\nFinal Answer: 你好，我是 AgentDesk 知识库助手。你可以直接问我公司制度、产品文档里的内容，也可以让我做计算或查询当前时间。";
        }
        if (TIME_INTENT.matcher(question).find()) {
            return "Thought: 用户想获取当前时间，调用 current_time 工具。\nAction: current_time\nAction Input: now";
        }
        if (STATS_INTENT.matcher(question).find()) {
            return "Thought: 用户想了解知识库规模，调用 knowledge_stats 工具。\nAction: knowledge_stats\nAction Input: summary";
        }
        String expression = extractExpression(question);
        if (expression != null) {
            return "Thought: 这是一个数学计算问题，调用 calculator 工具。\nAction: calculator\nAction Input: " + expression;
        }
        return "Thought: 需要先在企业知识库中检索相关资料。\nAction: knowledge_search\nAction Input: " + question;
    }

    private String extractExpression(String question) {
        String candidate = question.replaceAll("[^0-9+\\-*/%^().\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (candidate.length() < 3
                || !MATH_CHARS.matcher(candidate).matches()
                || !HAS_OPERATOR.matcher(candidate).find()
                || !HAS_DIGIT.matcher(candidate).find()) {
            return null;
        }
        // 去掉首尾的运算符/括号，避免生成非法表达式
        candidate = candidate.replaceAll("^[+*/%^).\\s]+", "").replaceAll("[+\\-*/%^(\\s]+$", "").trim();
        return candidate.length() < 3 ? null : candidate;
    }

    private String finalAnswer(String scratchpad, String question) {
        String action = lastLineValue(scratchpad, "Action:");
        String observation = lastObservation(scratchpad);
        if (observation.isBlank()) {
            return "Thought: 工具没有返回内容，直接说明情况。\nFinal Answer: 抱歉，这次没有拿到可用的资料，请换个问法再试一次。";
        }

        String answer;
        if ("calculator".equalsIgnoreCase(action)) {
            String expression = lastLineValue(scratchpad, "Action Input:");
            answer = "计算结果是：`" + expression + " = " + observation + "`。";
        } else if ("current_time".equalsIgnoreCase(action)) {
            answer = observation + "。";
        } else if ("knowledge_stats".equalsIgnoreCase(action)) {
            answer = observation;
        } else {
            answer = knowledgeAnswer(observation, question);
        }
        return "Thought: 已经拿到工具返回的结果，可以组织答案了。\nFinal Answer: " + answer;
    }

    /**
     * 抽取式回答：从命中片段中挑选与问题关键词重合度最高的句子，并标注出处。
     *
     * <p>规则模型不做生成式改写，但通过“句子级打分 + 引用来源”也能给出简洁、可核查的答案；
     * 换成真实大模型后，这里会由模型直接基于同样的片段生成自然语言回答。</p>
     */
    private String knowledgeAnswer(String observation, String question) {
        if (observation.startsWith("未检索到") || observation.startsWith("知识库")) {
            return "我在你的知识库中没有检索到直接相关的内容。可以尝试：\n"
                    + "1. 换一个更具体的关键词（如“差旅报销标准”）；\n"
                    + "2. 到「知识库」页上传相关文档后再问。";
        }
        List<Block> blocks = new ArrayList<>();
        for (String block : observation.split("\\n\\[")) {
            if (!block.contains("《")) {
                continue;
            }
            String[] lines = block.split("\\n", 2);
            if (lines.length < 2) {
                continue;
            }
            blocks.add(new Block(lines[0].trim().replaceAll("^\\d+\\]\\s*", ""), lines[1].trim()));
        }
        if (blocks.isEmpty()) {
            return observation;
        }

        Set<String> keywords = new HashSet<>(TextTokenizer.distinctTokens(question)).stream()
                .filter(token -> token.length() >= 2)
                .collect(Collectors.toSet());
        if (keywords.isEmpty()) {
            keywords = new HashSet<>(TextTokenizer.distinctTokens(question));
        }

        List<Sentence> candidates = new ArrayList<>();
        for (Block block : blocks) {
            for (String raw : block.content().split("(?<=[。！？!?])|\\n")) {
                String sentence = raw.replaceAll("^#+\\s*", "").replace("**", "").trim();
                if (sentence.length() < 12 || sentence.startsWith("Q：") || sentence.endsWith("？")
                        || sentence.endsWith("?")) {
                    continue;
                }
                long hits = keywords.stream().filter(sentence::contains).count();
                if (hits > 0) {
                    candidates.add(new Sentence(sentence, block.source(), hits / Math.sqrt(sentence.length())));
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(Sentence::score).reversed());

        StringBuilder sb = new StringBuilder("根据知识库检索结果：\n\n");
        Set<String> seen = new HashSet<>();
        int used = 0;
        double bestScore = candidates.isEmpty() ? 0 : candidates.get(0).score();
        for (Sentence candidate : candidates) {
            if (used >= 2) {
                break;
            }
            // 第二句必须足够相关，避免把弱相关句子拼进答案
            if (used > 0 && candidate.score() < bestScore * 0.75) {
                break;
            }
            String key = candidate.text().substring(0, Math.min(24, candidate.text().length()));
            if (!seen.add(key)) {
                continue;
            }
            sb.append("> ").append(candidate.text()).append("\n\n");
            used++;
        }
        if (used == 0) {
            String fallback = blocks.get(0).content().replaceAll("\\s+", " ").trim();
            sb.append("> ").append(fallback.length() > 160 ? fallback.substring(0, 160) + "…" : fallback)
                    .append("\n\n");
        }
        String source = candidates.isEmpty() ? blocks.get(0).source() : candidates.get(0).source();
        sb.append("—— 摘自 ").append(source).append("\n\n")
                .append("如需更完整的条款或具体场景的解释，可以继续追问。");
        return sb.toString();
    }

    private record Block(String source, String content) {
    }

    private record Sentence(String text, String source, double score) {
    }

    private String lastLineValue(String text, String field) {
        String value = "";
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(field)) {
                value = trimmed.substring(field.length()).trim();
            }
        }
        return value;
    }

    private String lastObservation(String text) {
        int index = text.lastIndexOf("Observation:");
        if (index < 0) {
            return "";
        }
        return text.substring(index + "Observation:".length()).trim();
    }

    /**
     * 取两个标记之间的内容；end 为 null 时取到结尾。
     */
    private String section(String prompt, String start, String end) {
        int startIndex = prompt.indexOf(start);
        if (startIndex < 0) {
            return null;
        }
        int from = startIndex + start.length();
        int to = end == null ? prompt.length() : prompt.indexOf(end, from);
        if (to < 0) {
            to = prompt.length();
        }
        return prompt.substring(from, Math.max(from, to)).trim();
    }

    @Override
    public String name() {
        return "mock-rule-model";
    }
}
