package com.agentdesk.agent.tools;

import com.agentdesk.agent.AgentContext;
import com.agentdesk.agent.Tool;
import com.agentdesk.config.AppProperties;
import com.agentdesk.retrieval.RetrievalService;
import com.agentdesk.retrieval.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库检索工具：Agent 的 RAG 入口，返回带来源与相关度的片段。
 */
@Component
@RequiredArgsConstructor
public class KnowledgeSearchTool implements Tool {

    private static final int MAX_OUTPUT_CHARS = 2600;

    private final RetrievalService retrievalService;
    private final AppProperties properties;

    @Override
    public String name() {
        return "knowledge_search";
    }

    @Override
    public String description() {
        return "检索企业知识库（用户上传的文档）。输入为一句自然语言问题或关键词，例如“差旅报销标准”；"
                + "返回最相关的若干片段及来源文档，答案必须基于返回内容。";
    }

    @Override
    public String execute(String input, AgentContext context) {
        List<RetrievedChunk> hits = retrievalService.search(context.userId(), input, properties.getRag().getTopK());
        if (hits.isEmpty()) {
            return "未检索到相关文档片段（知识库可能为空，或问题与已有文档无关）。";
        }
        StringBuilder sb = new StringBuilder("共命中 ").append(hits.size()).append(" 个片段：\n");
        for (int i = 0; i < hits.size(); i++) {
            RetrievedChunk hit = hits.get(i);
            String block = "\n[" + (i + 1) + "] 《" + hit.documentName() + "》 第 " + (hit.chunkIndex() + 1)
                    + " 段（相关度 " + String.format("%.2f", hit.score()) + "）\n"
                    + hit.content() + "\n";
            if (sb.length() + block.length() > MAX_OUTPUT_CHARS) {
                sb.append("\n（结果较长，已截断，仅保留高相关片段）");
                break;
            }
            sb.append(block);
        }
        return sb.toString().trim();
    }
}
