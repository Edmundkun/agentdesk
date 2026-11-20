package com.agentdesk.chat;

import com.agentdesk.chat.dto.ChatRequest;
import com.agentdesk.chat.dto.ConversationVO;
import com.agentdesk.chat.dto.CreateConversationRequest;
import com.agentdesk.chat.dto.MessageVO;
import com.agentdesk.common.ApiResponse;
import com.agentdesk.security.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Tag(name = "对话")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final long SSE_TIMEOUT_MS = 180_000L;

    private final ChatService chatService;
    private final ExecutorService agentExecutorPool;

    public ChatController(ChatService chatService,
                          @Qualifier("agentExecutorPool") ExecutorService agentExecutorPool) {
        this.chatService = chatService;
        this.agentExecutorPool = agentExecutorPool;
    }

    @Operation(summary = "会话列表")
    @GetMapping("/conversations")
    public ApiResponse<List<ConversationVO>> conversations() {
        return ApiResponse.ok(chatService.listConversations(AuthUtils.currentUserId()));
    }

    @Operation(summary = "新建会话")
    @PostMapping("/conversations")
    public ApiResponse<ConversationVO> create(@RequestBody(required = false) CreateConversationRequest request) {
        return ApiResponse.ok(chatService.createConversation(AuthUtils.currentUserId(),
                request == null ? null : request.title()));
    }

    @Operation(summary = "删除会话（级联删除消息）")
    @DeleteMapping("/conversations/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        chatService.deleteConversation(AuthUtils.currentUserId(), id);
        return ApiResponse.ok();
    }

    @Operation(summary = "会话消息列表（含 Agent 推理轨迹）")
    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<MessageVO>> messages(@PathVariable Long id) {
        return ApiResponse.ok(chatService.listMessages(AuthUtils.currentUserId(), id));
    }

    /**
     * SSE 流式对话：在独立线程池中执行 Agent，避免慢推理占满 Tomcat 请求线程。
     */
    @Operation(summary = "流式对话（SSE）")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatRequest request) {
        Long userId = AuthUtils.currentUserId();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);
        emitter.onError(throwable -> emitter.complete());
        agentExecutorPool.execute(() -> chatService.stream(userId, request, emitter));
        return emitter;
    }
}
