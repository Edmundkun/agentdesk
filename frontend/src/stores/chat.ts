import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import {
  deleteConversation as deleteConversationApi,
  getConversations,
  getMessages,
  isAbortError,
  streamChat,
} from '../api/chat'
import type { ChatMessage, Conversation, TraceStep } from '../api/types'

export const useChatStore = defineStore('chat', () => {
  const conversations = ref<Conversation[]>([])
  const activeId = ref<number | null>(null)
  const messages = ref<ChatMessage[]>([])
  const steps = ref<TraceStep[]>([])
  const streaming = ref(false)
  const loadingMessages = ref(false)
  const loadingConversations = ref(false)
  const lastDuration = ref<number | null>(null)

  /** 流式请求序号：切换会话 / 新建会话时递增，用于丢弃过期流的回调 */
  let streamSeq = 0
  let abortController: AbortController | null = null

  const activeConversation = computed<Conversation | null>(
    () => conversations.value.find((item) => item.id === activeId.value) ?? null,
  )

  const lastAssistantMessage = computed<ChatMessage | null>(() => {
    for (let i = messages.value.length - 1; i >= 0; i -= 1) {
      const item = messages.value[i]
      if (item && item.role === 'assistant') return item
    }
    return null
  })

  /** 轨迹面板数据：流式时用实时步骤，否则回落到最近一条助手消息的历史轨迹 */
  const traceSteps = computed<TraceStep[]>(() => {
    if (streaming.value || steps.value.length > 0) return steps.value
    return lastAssistantMessage.value?.trace ?? []
  })

  const traceDuration = computed<number>(() => {
    if (lastDuration.value !== null) return lastDuration.value
    return traceSteps.value.reduce((total, step) => total + (step.latencyMs || 0), 0)
  })

  function currentAssistant(): ChatMessage | null {
    const last = messages.value[messages.value.length - 1]
    return last && last.role === 'assistant' ? last : null
  }

  async function loadConversations(): Promise<void> {
    loadingConversations.value = true
    try {
      conversations.value = await getConversations()
    } catch {
      /* 拦截器已提示错误 */
    } finally {
      loadingConversations.value = false
    }
  }

  async function selectConversation(id: number): Promise<void> {
    if (activeId.value === id && messages.value.length > 0) return
    stopStreaming()
    streamSeq += 1
    activeId.value = id
    steps.value = []
    lastDuration.value = null
    loadingMessages.value = true
    try {
      messages.value = await getMessages(id)
    } catch {
      messages.value = []
    } finally {
      loadingMessages.value = false
    }
  }

  function startNewConversation(): void {
    stopStreaming()
    streamSeq += 1
    activeId.value = null
    messages.value = []
    steps.value = []
    lastDuration.value = null
  }

  async function removeConversation(id: number): Promise<void> {
    await deleteConversationApi(id)
    conversations.value = conversations.value.filter((item) => item.id !== id)
    if (activeId.value === id) startNewConversation()
  }

  function stopStreaming(): void {
    abortController?.abort()
  }

  async function sendMessage(rawQuestion: string): Promise<void> {
    const question = rawQuestion.trim()
    if (!question || streaming.value) return

    streamSeq += 1
    const seq = streamSeq
    const isStale = (): boolean => seq !== streamSeq

    const stamp = new Date().toISOString()
    messages.value.push(
      {
        id: `local-user-${stamp}`,
        role: 'user',
        content: question,
        trace: null,
        createdAt: stamp,
      },
      {
        id: `local-assistant-${stamp}`,
        role: 'assistant',
        content: '',
        trace: null,
        createdAt: stamp,
        streaming: true,
      },
    )

    steps.value = []
    lastDuration.value = null
    streaming.value = true

    const controller = new AbortController()
    abortController = controller
    const requestConversationId = activeId.value

    try {
      await streamChat({
        conversationId: requestConversationId,
        question,
        signal: controller.signal,
        handlers: {
          onMeta: (meta) => {
            if (isStale()) return
            // 新会话由后端创建：刷新列表并选中它，但不重新拉取消息（流仍在进行）
            if (requestConversationId === null && activeId.value === null && meta.conversationId) {
              activeId.value = meta.conversationId
              void loadConversations()
            }
          },
          onStep: (step) => {
            if (isStale()) return
            steps.value.push(step)
          },
          onToken: (text) => {
            if (isStale() || !text) return
            const target = currentAssistant()
            if (target) target.content += text
          },
          onDone: (payload) => {
            if (isStale()) return
            const target = currentAssistant()
            if (target) {
              if (!target.content && payload.answer) target.content = payload.answer
              target.trace = steps.value.length > 0 ? [...steps.value] : null
              target.streaming = false
            }
            lastDuration.value = payload.durationMs > 0 ? payload.durationMs : null
            void loadConversations()
          },
          onError: (message) => {
            if (isStale()) return
            const target = currentAssistant()
            if (target) {
              target.streaming = false
              if (!target.content.trim()) target.content = `⚠️ ${message}`
              target.trace = steps.value.length > 0 ? [...steps.value] : null
            }
            ElMessage.error(message)
          },
        },
      })
    } catch (error) {
      if (!isStale()) {
        const target = currentAssistant()
        if (target) {
          target.streaming = false
          target.trace = steps.value.length > 0 ? [...steps.value] : null
        }
        if (isAbortError(error)) {
          if (target && !target.content.trim()) target.content = '（已停止生成）'
          ElMessage.info('已停止生成')
        } else {
          const message = error instanceof Error ? error.message : '对话失败，请稍后重试'
          if (target && !target.content.trim()) target.content = `⚠️ ${message}`
          ElMessage.error(message)
        }
      }
    } finally {
      if (abortController === controller) abortController = null
      if (!isStale()) {
        streaming.value = false
        const target = currentAssistant()
        if (target) target.streaming = false
      }
    }
  }

  function reset(): void {
    stopStreaming()
    streamSeq += 1
    conversations.value = []
    activeId.value = null
    messages.value = []
    steps.value = []
    lastDuration.value = null
    streaming.value = false
    loadingMessages.value = false
  }

  return {
    conversations,
    activeId,
    messages,
    steps,
    streaming,
    loadingMessages,
    loadingConversations,
    lastDuration,
    activeConversation,
    lastAssistantMessage,
    traceSteps,
    traceDuration,
    loadConversations,
    selectConversation,
    startNewConversation,
    removeConversation,
    stopStreaming,
    sendMessage,
    reset,
  }
})
