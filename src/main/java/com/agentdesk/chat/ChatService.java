package com.agentdesk.chat;

import com.agentdesk.agent.AgentContext;
import com.agentdesk.agent.AgentExecutor;
import com.agentdesk.agent.AgentResult;
import com.agentdesk.agent.AgentStep;
import com.agentdesk.agent.HistoryMessage;
import com.agentdesk.chat.dto.ChatRequest;
import com.agentdesk.chat.dto.ConversationVO;
import com.agentdesk.chat.dto.MessageVO;
import com.agentdesk.common.BizException;
import com.agentdesk.config.AppProperties;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 会话服务：负责会话/消息持久化与 SSE 流式对话编排。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final AgentExecutor agentExecutor;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;

    public List<ConversationVO> listConversations(Long userId) {
        return conversationMapper.selectList(Wrappers.<Conversation>lambdaQuery()
                        .eq(Conversation::getUserId, userId)
                        .orderByDesc(Conversation::getUpdatedAt))
                .stream()
                .map(conversation -> ConversationVO.from(conversation, countMessages(conversation.getId())))
                .toList();
    }

    public ConversationVO createConversation(Long userId, String title) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(normalizeTitle(title));
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(conversation.getCreatedAt());
        conversationMapper.insert(conversation);
        return ConversationVO.from(conversation, 0);
    }

    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        Conversation conversation = requireConversation(userId, conversationId);
        messageMapper.delete(Wrappers.<Message>lambdaQuery()
                .eq(Message::getConversationId, conversation.getId()));
        conversationMapper.deleteById(conversation.getId());
    }

    public List<MessageVO> listMessages(Long userId, Long conversationId) {
        Conversation conversation = requireConversation(userId, conversationId);
        return messageMapper.selectList(Wrappers.<Message>lambdaQuery()
                        .eq(Message::getConversationId, conversation.getId())
                        .orderByAsc(Message::getId))
                .stream()
                .map(this::toMessageVO)
                .toList();
    }

    /**
     * 流式对话主流程：持久化用户消息 → Agent 推理 → 逐步推送轨迹 → 分片推送答案 → 持久化助手消息。
     */
    public void stream(Long userId, ChatRequest request, SseEmitter emitter) {
        long start = System.currentTimeMillis();
        try {
            Conversation conversation = resolveConversation(userId, request.conversationId(), request.question());
            Message userMessage = saveMessage(conversation.getId(), "user", request.question(), null);
            touch(conversation);

            send(emitter, "meta", Map.of("conversationId", conversation.getId(),
                    "userMessageId", userMessage.getId()));

            List<HistoryMessage> history = loadHistory(conversation.getId(), userMessage.getId());
            AgentResult result = agentExecutor.execute(
                    new AgentContext(userId, conversation.getId()), request.question(), history);

            for (AgentStep step : result.steps()) {
                send(emitter, "step", step);
            }

            Message assistantMessage = saveMessage(conversation.getId(), "assistant", result.answer(),
                    objectMapper.writeValueAsString(result.steps()));
            touch(conversation);

            streamAnswer(emitter, result.answer());

            send(emitter, "done", Map.of("messageId", assistantMessage.getId(),
                    "answer", result.answer(),
                    "durationMs", System.currentTimeMillis() - start,
                    "stepCount", result.steps().size()));
            emitter.complete();
        } catch (Exception e) {
            log.error("流式对话失败 userId={}", userId, e);
            try {
                send(emitter, "error", Map.of("message",
                        e.getMessage() == null ? "对话失败，请稍后重试" : e.getMessage()));
            } catch (Exception ignored) {
                // 客户端可能已断开，忽略
            }
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    private void streamAnswer(SseEmitter emitter, String answer) throws IOException, InterruptedException {
        for (String piece : splitByCodePoints(answer, 6)) {
            send(emitter, "token", Map.of("text", piece));
            Thread.sleep(10);
        }
    }

    private List<String> splitByCodePoints(String text, int size) {
        List<String> pieces = new ArrayList<>();
        int index = 0;
        while (index < text.length()) {
            int count = Math.min(size, text.codePointCount(index, text.length()));
            int end = text.offsetByCodePoints(index, count);
            pieces.add(text.substring(index, end));
            index = end;
        }
        return pieces;
    }

    private Conversation resolveConversation(Long userId, Long conversationId, String question) {
        if (conversationId != null) {
            Conversation conversation = requireConversation(userId, conversationId);
            // 通过「新建对话」入口创建的会话没有消息，用首个问题补上标题
            if (countMessages(conversation.getId()) == 0) {
                conversation.setTitle(normalizeTitle(question));
                conversationMapper.updateById(conversation);
            }
            return conversation;
        }
        Conversation created = new Conversation();
        created.setUserId(userId);
        created.setTitle(normalizeTitle(question));
        created.setCreatedAt(LocalDateTime.now());
        created.setUpdatedAt(created.getCreatedAt());
        conversationMapper.insert(created);
        return created;
    }

    private Conversation requireConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw BizException.notFound("会话不存在");
        }
        return conversation;
    }

    private List<HistoryMessage> loadHistory(Long conversationId, Long beforeMessageId) {
        int size = Math.max(0, properties.getAgent().getHistorySize());
        if (size == 0) {
            return List.of();
        }
        List<Message> messages = messageMapper.selectList(Wrappers.<Message>lambdaQuery()
                .eq(Message::getConversationId, conversationId)
                .lt(Message::getId, beforeMessageId)
                .orderByDesc(Message::getId)
                .last("LIMIT " + size));
        Collections.reverse(messages);
        return messages.stream()
                .filter(message -> message.getContent() != null && !message.getContent().isBlank())
                .map(message -> new HistoryMessage(message.getRole(), message.getContent()))
                .toList();
    }

    private Message saveMessage(Long conversationId, String role, String content, String traceJson) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content == null ? "" : content);
        message.setTraceJson(traceJson);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        return message;
    }

    private void touch(Conversation conversation) {
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
    }

    private long countMessages(Long conversationId) {
        Long count = messageMapper.selectCount(Wrappers.<Message>lambdaQuery()
                .eq(Message::getConversationId, conversationId));
        return count == null ? 0 : count;
    }

    private MessageVO toMessageVO(Message message) {
        List<AgentStep> trace = null;
        if (message.getTraceJson() != null && !message.getTraceJson().isBlank()) {
            try {
                trace = objectMapper.readValue(message.getTraceJson(), new TypeReference<List<AgentStep>>() {
                });
            } catch (Exception e) {
                log.warn("解析推理轨迹失败 messageId={}", message.getId(), e);
            }
        }
        return new MessageVO(message.getId(), message.getRole(), message.getContent(), trace, message.getCreatedAt());
    }

    private String normalizeTitle(String title) {
        String value = title == null || title.isBlank() ? "新对话" : title.trim().replaceAll("\\s+", " ");
        return value.length() > 30 ? value.substring(0, 30) : value;
    }

    private void send(SseEmitter emitter, String event, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
    }
}
