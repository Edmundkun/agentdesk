import { clearSession, redirectToLogin, request, TOKEN_KEY } from './http'
import { createSseDecoder, dispatchSseFrame } from './sse'
import type { StreamHandlers } from './sse'
import type { ChatMessage, Conversation } from './types'

/* --------------------------- 会话 REST 接口 --------------------------- */

/** GET /chat/conversations */
export function getConversations(): Promise<Conversation[]> {
  return request<Conversation[]>({ url: '/chat/conversations', method: 'get' })
}

/** POST /chat/conversations */
export function createConversation(title = '新会话'): Promise<Conversation> {
  return request<Conversation>({
    url: '/chat/conversations',
    method: 'post',
    data: { title },
  })
}

/** DELETE /chat/conversations/{id} */
export function deleteConversation(id: number): Promise<null> {
  return request<null>({ url: `/chat/conversations/${id}`, method: 'delete' })
}

/** GET /chat/conversations/{id}/messages */
export function getMessages(conversationId: number): Promise<ChatMessage[]> {
  return request<ChatMessage[]>({
    url: `/chat/conversations/${conversationId}/messages`,
    method: 'get',
  })
}

/* ----------------------------- SSE 流式对话 ---------------------------- */

export interface StreamChatParams {
  conversationId: number | null
  question: string
  signal?: AbortSignal
  handlers: StreamHandlers
}

export function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError'
}

/**
 * POST /chat/stream：用 fetch 读取 SSE 流。
 * EventSource 不支持 POST 与自定义 Header，因此手写 ReadableStream 解析。
 */
export async function streamChat({
  conversationId,
  question,
  signal,
  handlers,
}: StreamChatParams): Promise<void> {
  const token = localStorage.getItem(TOKEN_KEY)
  const response = await fetch('/api/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ conversationId, question }),
    signal,
  })

  if (response.status === 401) {
    clearSession()
    redirectToLogin()
    throw new Error('登录已过期，请重新登录')
  }

  if (!response.ok || !response.body) {
    const text = await response.text().catch(() => '')
    let message = `对话请求失败（HTTP ${response.status}）`
    try {
      const parsed = JSON.parse(text) as { message?: unknown }
      if (typeof parsed.message === 'string' && parsed.message) message = parsed.message
    } catch {
      /* 非 JSON 响应体，使用默认提示 */
    }
    throw new Error(message)
  }

  await readEventStream(response.body, handlers, signal)
}

async function readEventStream(
  body: ReadableStream<Uint8Array>,
  handlers: StreamHandlers,
  signal?: AbortSignal,
): Promise<void> {
  const reader = body.getReader()
  const decoder = new TextDecoder('utf-8')
  const sse = createSseDecoder()
  let terminal = false

  const cancelOnAbort = (): void => {
    void reader.cancel().catch(() => undefined)
  }
  signal?.addEventListener('abort', cancelOnAbort, { once: true })

  try {
    while (!terminal) {
      const { done, value } = await reader.read()
      if (done) break

      for (const frame of sse.push(decoder.decode(value, { stream: true }))) {
        if (dispatchSseFrame(frame, handlers)) {
          terminal = true
          break
        }
      }
    }

    if (!terminal) {
      for (const frame of sse.end(decoder.decode())) {
        if (dispatchSseFrame(frame, handlers)) {
          terminal = true
          break
        }
      }
    }
  } finally {
    signal?.removeEventListener('abort', cancelOnAbort)
    if (terminal) {
      void reader.cancel().catch(() => undefined)
    } else {
      reader.releaseLock()
    }
  }
}
