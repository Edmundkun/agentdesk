package com.agentdesk.agent;

import com.agentdesk.common.BizException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具注册表：Spring 自动注入所有 {@link Tool} 实现，按名称路由调用。
 */
@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public ToolRegistry(List<Tool> toolBeans) {
        for (Tool tool : toolBeans) {
            if (tools.putIfAbsent(tool.name(), tool) != null) {
                throw new IllegalStateException("工具名重复：" + tool.name());
            }
        }
    }

    public String execute(String name, String input, AgentContext context) throws Exception {
        Tool tool = tools.get(name);
        if (tool == null) {
            throw BizException.badRequest("未知工具：" + name + "，可用工具：" + String.join(", ", tools.keySet()));
        }
        return tool.execute(input == null ? "" : input.trim(), context);
    }

    /**
     * 生成 Prompt 中的工具清单。
     */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        tools.values().forEach(tool -> sb.append("- ")
                .append(tool.name())
                .append("：")
                .append(tool.description())
                .append('\n'));
        return sb.toString().trim();
    }

    public int size() {
        return tools.size();
    }
}
