package com.agentdesk.agent;

import com.agentdesk.config.AppProperties;
import com.agentdesk.llm.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自研 ReAct Agent 执行器。
 *
 * <p>核心循环：拼装 Prompt → 模型输出 Thought/Action → 调用工具 → Observation 回填 → 直到 Final Answer。
 * 相比直接引入 Agent 框架，这里把「工具注册、推理循环、轨迹记录、死循环保护」都显式实现，
 * 便于按业务定制，也便于单元测试。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentExecutor {

    private static final Pattern FINAL_PATTERN =
            Pattern.compile("(?i)final\\s*answer\\s*[:：]\\s*");
    private static final Pattern ACTION_PATTERN =
            Pattern.compile("(?i)^\\W*action\\s*[:：]\\s*(.+)$");
    private static final Pattern ACTION_INPUT_PATTERN =
            Pattern.compile("(?i)^\\W*action\\s*input\\s*[:：]\\s*(.+)$");
    private static final Pattern THOUGHT_PATTERN =
            Pattern.compile("(?i)^\\W*thought\\s*[:：]\\s*(.+)$");
    private static final int MAX_OBSERVATION_IN_PROMPT = 1800;
    private static final int MAX_OBSERVATION_IN_STEP = 800;

    private final ChatModel chatModel;
    private final ToolRegistry toolRegistry;
    private final AppProperties properties;

    public AgentResult execute(AgentContext context, String question, List<HistoryMessage> history) {
        long start = System.currentTimeMillis();
        int maxSteps = Math.max(1, properties.getAgent().getMaxSteps());
        List<AgentStep> steps = new ArrayList<>();
        StringBuilder scratchpad = new StringBuilder();
        String lastObservation = "";

        for (int round = 1; round <= maxSteps; round++) {
            String prompt = buildPrompt(question, history, scratchpad.toString(), maxSteps);
            String output = chatModel.chat(prompt);
            if (output == null || output.isBlank()) {
                break;
            }

            String finalAnswer = extractFinal(output);
            if (finalAnswer != null && !finalAnswer.isBlank()) {
                return new AgentResult(finalAnswer.trim(), steps, elapsed(start));
            }

            String action = extractField(output, ACTION_PATTERN);
            String actionInput = extractField(output, ACTION_INPUT_PATTERN);
            if (action == null) {
                // 模型没有按格式给出 Action，退化为直接回答
                return new AgentResult(output.trim(), steps, elapsed(start));
            }

            String thought = extractField(output, THOUGHT_PATTERN);
            long toolStart = System.currentTimeMillis();
            String observation;
            try {
                observation = toolRegistry.execute(action, actionInput,
                        new AgentContext(context.userId(), context.conversationId()));
            } catch (Exception e) {
                log.warn("工具执行失败 tool={} input={} error={}", action, actionInput, e.getMessage());
                observation = "工具执行失败：" + e.getMessage();
            }
            long latency = System.currentTimeMillis() - toolStart;
            lastObservation = observation;

            steps.add(new AgentStep(round, thought, action, actionInput,
                    truncate(observation, MAX_OBSERVATION_IN_STEP), latency));
            scratchpad.append("Thought: ").append(thought == null ? "" : thought).append('\n')
                    .append("Action: ").append(action).append('\n')
                    .append("Action Input: ").append(actionInput == null ? "" : actionInput).append('\n')
                    .append("Observation: ").append(truncate(observation, MAX_OBSERVATION_IN_PROMPT))
                    .append("\n\n");

            if (isRepeating(steps)) {
                log.info("检测到重复工具调用，提前结束 agent 循环 conversationId={}", context.conversationId());
                break;
            }
        }

        String answer = lastObservation.isBlank()
                ? "抱歉，我暂时没有找到合适的答案。可以换一种问法，或先在「知识库」中上传相关文档。"
                : "抱歉，我在 " + steps.size() + " 轮推理内没有得出确定结论。最后一次工具返回的结果是：\n\n"
                + truncate(lastObservation, MAX_OBSERVATION_IN_STEP);
        return new AgentResult(answer, steps, elapsed(start));
    }

    private boolean isRepeating(List<AgentStep> steps) {
        if (steps.size() < 2) {
            return false;
        }
        AgentStep last = steps.get(steps.size() - 1);
        AgentStep previous = steps.get(steps.size() - 2);
        return last.getTool().equals(previous.getTool())
                && String.valueOf(last.getInput()).equals(String.valueOf(previous.getInput()));
    }

    private String buildPrompt(String question, List<HistoryMessage> history, String scratchpad, int maxSteps) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("你是 AgentDesk，一个企业知识库智能助手。你可以调用工具获取事实，禁止编造答案。\n\n");
        sb.append("可用工具：\n").append(toolRegistry.describe()).append("\n\n");
        sb.append("请严格按 ReAct 格式输出，每轮只输出一个 Action：\n");
        sb.append("Thought: 简要思考\n")
                .append("Action: 工具名\n")
                .append("Action Input: 工具输入（单行）\n\n");
        sb.append("信息足够后输出：\n")
                .append("Thought: 简要思考\n")
                .append("Final Answer: 中文最终答案（引用文档名，不要输出工具调用过程）\n\n");
        sb.append("约束：\n")
                .append("1. 涉及公司制度、产品、流程的问题，必须先调用 knowledge_search，禁止编造；\n")
                .append("2. 数学计算必须调用 calculator，不要心算；\n")
                .append("3. 最多 ").append(maxSteps).append(" 轮工具调用；\n")
                .append("4. Final Answer 使用简洁中文，必要时用有序列表。\n\n");

        sb.append(AgentPrompts.HISTORY_HEADER).append('\n');
        if (history == null || history.isEmpty()) {
            sb.append(AgentPrompts.EMPTY);
        } else {
            history.forEach(message -> sb.append("user".equals(message.role()) ? "用户: " : "助手: ")
                    .append(message.content()).append('\n'));
        }
        sb.append('\n')
                .append(AgentPrompts.QUESTION_HEADER).append('\n')
                .append(question == null ? "" : question.trim()).append("\n\n")
                .append(AgentPrompts.SCRATCHPAD_HEADER).append('\n')
                .append(scratchpad == null || scratchpad.isBlank() ? AgentPrompts.EMPTY : scratchpad);
        return sb.toString();
    }

    private String extractFinal(String output) {
        Matcher matcher = FINAL_PATTERN.matcher(output);
        if (matcher.find()) {
            return output.substring(matcher.end()).trim();
        }
        return null;
    }

    private String extractField(String output, Pattern pattern) {
        for (String line : output.split("\\R")) {
            Matcher matcher = pattern.matcher(line.replace("**", "").trim());
            if (matcher.matches()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "…（已截断）";
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}
