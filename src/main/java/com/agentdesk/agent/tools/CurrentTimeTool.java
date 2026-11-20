package com.agentdesk.agent.tools;

import com.agentdesk.agent.AgentContext;
import com.agentdesk.agent.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 当前时间工具：处理“现在几点/今天几号”这类不需要知识库的问题。
 */
@Component
public class CurrentTimeTool implements Tool {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEEE", Locale.SIMPLIFIED_CHINESE);

    @Override
    public String name() {
        return "current_time";
    }

    @Override
    public String description() {
        return "获取当前日期和时间（Asia/Shanghai）。输入可为空或任意占位符。";
    }

    @Override
    public String execute(String input, AgentContext context) {
        return "当前时间：" + LocalDateTime.now(ZONE).format(FORMATTER);
    }
}
