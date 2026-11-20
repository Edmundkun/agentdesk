package com.agentdesk.agent.tools;

import com.agentdesk.agent.AgentContext;
import com.agentdesk.agent.Tool;
import com.agentdesk.knowledge.Document;
import com.agentdesk.knowledge.DocumentChunk;
import com.agentdesk.knowledge.DocumentChunkMapper;
import com.agentdesk.knowledge.DocumentMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 知识库统计工具：回答“知识库里有多少资料”这类元问题。
 */
@Component
@RequiredArgsConstructor
public class KnowledgeStatsTool implements Tool {

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper chunkMapper;

    @Override
    public String name() {
        return "knowledge_stats";
    }

    @Override
    public String description() {
        return "查询当前用户知识库的规模：文档数量、切片数量、累计字数。输入可为空或任意占位符。";
    }

    @Override
    public String execute(String input, AgentContext context) {
        Long userId = context.userId();
        Long documents = documentMapper.selectCount(Wrappers.<Document>lambdaQuery()
                .eq(Document::getUserId, userId));
        Long chunks = chunkMapper.selectCount(Wrappers.<DocumentChunk>lambdaQuery()
                .eq(DocumentChunk::getUserId, userId));
        long chars = chunkMapper.sumContentLength(userId);
        return "知识库中共有 " + (documents == null ? 0 : documents) + " 份文档、"
                + (chunks == null ? 0 : chunks) + " 个切片，累计约 " + chars + " 字。";
    }
}
