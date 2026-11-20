<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound,
  CircleClose,
  DArrowLeft,
  DArrowRight,
  Delete,
  Document,
  MagicStick,
  Plus,
  Promotion,
  Search,
} from '@element-plus/icons-vue'
import type { Component } from 'vue'
import AppShell from '../components/AppShell.vue'
import AgentTracePanel from '../components/AgentTracePanel.vue'
import EmptyState from '../components/EmptyState.vue'
import MessageBubble from '../components/MessageBubble.vue'
import { useChatStore } from '../stores/chat'
import type { Conversation } from '../api/types'
import { formatDateTime, formatRelative } from '../utils/format'

interface ExampleQuestion {
  icon: Component
  title: string
  question: string
}

const chat = useChatStore()

const draft = ref('')
const traceCollapsed = ref(false)
const scrollRef = ref<HTMLDivElement | null>(null)

const examples: ExampleQuestion[] = [
  {
    icon: Document,
    title: '制度查询',
    question: '差旅报销需要在多久内提交？需要准备哪些材料？',
  },
  {
    icon: Search,
    title: '流程梳理',
    question: '新员工入职第一周需要完成哪些事项？',
  },
  {
    icon: MagicStick,
    title: '要点总结',
    question: '请总结公司的请假制度，并列出不同假期的天数。',
  },
]

const llmMode = (import.meta.env.VITE_LLM_MODE ?? 'mock').toLowerCase()
const modeLabel = computed<string>(() => (llmMode === 'llm' ? 'LLM' : '本地规则模型'))
const modeTitle = computed<string>(() =>
  llmMode === 'llm' ? 'VITE_LLM_MODE=llm · 大模型推理' : 'VITE_LLM_MODE=mock · 本地规则模型',
)

const headerTitle = computed<string>(() => chat.activeConversation?.title || '新对话')

const headerSubtitle = computed<string>(() => {
  const conversation = chat.activeConversation
  if (!conversation) return '上传文档后，向 Agent 提出你的第一个问题'
  return `创建于 ${formatDateTime(conversation.createdAt)} · 共 ${conversation.messageCount} 条消息`
})

const tailSignature = computed<string>(() => {
  const last = chat.messages[chat.messages.length - 1]
  return `${chat.messages.length}:${last ? last.content.length : 0}`
})

let scrollFrame = 0
function scrollToBottom(): void {
  if (scrollFrame) return
  scrollFrame = requestAnimationFrame(() => {
    scrollFrame = 0
    const element = scrollRef.value
    if (element) element.scrollTop = element.scrollHeight
  })
}

watch(tailSignature, scrollToBottom, { flush: 'post' })
watch(
  () => chat.activeId,
  () => {
    void nextTick(scrollToBottom)
  },
)

async function send(): Promise<void> {
  const question = draft.value.trim()
  if (!question || chat.streaming) return
  draft.value = ''
  await chat.sendMessage(question)
}

function handleExample(question: string): void {
  if (chat.streaming) return
  void chat.sendMessage(question)
}

function handleKeydown(event: Event | KeyboardEvent): void {
  if (!(event instanceof KeyboardEvent)) return
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing) return
  event.preventDefault()
  void send()
}

async function selectConversation(id: number): Promise<void> {
  if (chat.streaming) chat.stopStreaming()
  await chat.selectConversation(id)
  void nextTick(scrollToBottom)
}

async function handleDelete(conversation: Conversation): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `删除会话「${conversation.title || '新对话'}」后将无法恢复，确定继续吗？`,
      '删除会话',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
      },
    )
  } catch {
    return
  }
  try {
    await chat.removeConversation(conversation.id)
    ElMessage.success('会话已删除')
  } catch {
    /* 拦截器已提示错误 */
  }
}

onMounted(async () => {
  await chat.loadConversations()
  scrollToBottom()
})
</script>

<template>
  <AppShell>
    <template #sidebar>
      <div class="conv-panel">
        <button class="new-chat-btn" type="button" @click="chat.startNewConversation()">
          <el-icon :size="14"><Plus /></el-icon>
          <span>新建对话</span>
        </button>

        <div class="conv-label">
          <span>最近会话</span>
          <span class="conv-count">{{ chat.conversations.length }}</span>
        </div>

        <div class="conv-list">
          <button
            v-for="conversation in chat.conversations"
            :key="conversation.id"
            type="button"
            class="conv-item"
            :class="{ 'is-active': conversation.id === chat.activeId }"
            @click="selectConversation(conversation.id)"
          >
            <el-icon class="conv-icon" :size="14"><ChatDotRound /></el-icon>
            <span class="conv-main">
              <span class="conv-title">{{ conversation.title || '新对话' }}</span>
              <span class="conv-meta">
                {{ formatRelative(conversation.updatedAt) }} · {{ conversation.messageCount }} 条
              </span>
            </span>
            <span
              class="conv-delete"
              role="button"
              tabindex="0"
              aria-label="删除会话"
              @click.stop="handleDelete(conversation)"
              @keydown.enter.stop="handleDelete(conversation)"
            >
              <el-icon :size="13"><Delete /></el-icon>
            </span>
          </button>

          <p v-if="!chat.conversations.length" class="conv-empty">
            还没有会话记录<br />点击「新建对话」开始
          </p>
        </div>
      </div>
    </template>

    <div class="chat-view">
      <section class="chat-main">
        <header class="chat-header">
          <div class="chat-heading">
            <h1>{{ headerTitle }}</h1>
            <p>{{ headerSubtitle }}</p>
          </div>
          <div class="chat-actions">
            <span class="mode-chip" :title="modeTitle">
              <span class="mode-dot"></span>
              {{ modeLabel }}
            </span>
            <el-tooltip
              :content="traceCollapsed ? '展开轨迹面板' : '收起轨迹面板'"
              placement="bottom"
              :show-after="300"
            >
              <button
                class="icon-btn trace-toggle"
                type="button"
                aria-label="切换轨迹面板"
                @click="traceCollapsed = !traceCollapsed"
              >
                <el-icon :size="16">
                  <component :is="traceCollapsed ? DArrowLeft : DArrowRight" />
                </el-icon>
              </button>
            </el-tooltip>
          </div>
        </header>

        <div ref="scrollRef" class="chat-scroll">
          <div
            class="chat-scroll-inner"
            :class="{ 'is-empty': !chat.loadingMessages && chat.messages.length === 0 }"
          >
            <el-skeleton v-if="chat.loadingMessages" class="message-skeleton" :rows="4" animated />
            <EmptyState
              v-else-if="chat.messages.length === 0"
              :examples="examples"
              @select="handleExample"
            />
            <div v-else class="message-list">
              <MessageBubble
                v-for="message in chat.messages"
                :key="message.id"
                :message="message"
              />
            </div>
          </div>
        </div>

        <div class="composer">
          <div class="composer-box">
            <el-input
              v-model="draft"
              type="textarea"
              resize="none"
              :autosize="{ minRows: 1, maxRows: 6 }"
              :disabled="chat.streaming"
              placeholder="输入问题，Enter 发送 / Shift + Enter 换行"
              @keydown="handleKeydown"
            />
            <div class="composer-bar">
              <span class="composer-hint">
                <el-icon :size="12"><MagicStick /></el-icon>
                Agent 会先检索知识库，再给出带推理轨迹的答案
              </span>
              <div class="composer-actions">
                <el-button v-if="chat.streaming" class="stop-btn" @click="chat.stopStreaming()">
                  <el-icon :size="14"><CircleClose /></el-icon>
                  <span>停止生成</span>
                </el-button>
                <el-button v-else type="primary" :disabled="!draft.trim()" @click="send">
                  <el-icon class="send-icon" :size="14"><Promotion /></el-icon>
                  <span>发送</span>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <AgentTracePanel
        v-if="!traceCollapsed"
        :steps="chat.traceSteps"
        :streaming="chat.streaming"
        :duration-ms="chat.traceDuration"
        @collapse="traceCollapsed = true"
      />
      <button
        v-else
        type="button"
        class="trace-restore"
        aria-label="展开运行轨迹"
        @click="traceCollapsed = false"
      >
        <el-icon :size="14"><DArrowLeft /></el-icon>
        <span>运行轨迹</span>
      </button>
    </div>
  </AppShell>
</template>

<style scoped>
.chat-view {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: var(--color-content);
}

.chat-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

/* ------------------------------ 左侧会话列表 ------------------------------ */
.conv-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  width: 100%;
  height: 38px;
  margin: 4px 0 14px;
  border: 1px dashed rgba(148, 163, 184, 0.34);
  border-radius: 9px;
  background: rgba(99, 102, 241, 0.1);
  color: #c7d2fe;
  font-size: 13px;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease;
}

.new-chat-btn:hover {
  border-color: rgba(165, 180, 252, 0.55);
  background: rgba(99, 102, 241, 0.2);
  color: #e0e7ff;
}

.conv-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px 8px;
  color: #64748b;
  font-size: 10.5px;
  font-weight: 500;
  letter-spacing: 0.6px;
}

.conv-count {
  padding: 0 5px;
  border-radius: 6px;
  background: rgba(148, 163, 184, 0.12);
  color: #94a3b8;
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.conv-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 9px 8px 9px 10px;
  margin-bottom: 2px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: #cbd5e1;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.conv-item:hover {
  background: rgba(148, 163, 184, 0.09);
}

.conv-item.is-active {
  background: rgba(99, 102, 241, 0.18);
}

.conv-icon {
  flex: 0 0 auto;
  color: #64748b;
}

.conv-item.is-active .conv-icon {
  color: #a5b4fc;
}

.conv-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  line-height: 1.4;
}

.conv-title {
  overflow: hidden;
  color: #cbd5e1;
  font-size: 12.8px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-item.is-active .conv-title {
  color: #e5e7ff;
  font-weight: 500;
}

.conv-meta {
  overflow: hidden;
  margin-top: 1px;
  color: #5b6a80;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-delete {
  display: flex;
  flex: 0 0 22px;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 6px;
  color: #64748b;
  opacity: 0;
  transition: opacity 0.15s ease, background-color 0.15s ease, color 0.15s ease;
}

.conv-item:hover .conv-delete {
  opacity: 1;
}

.conv-delete:hover {
  background: rgba(239, 68, 68, 0.16);
  color: #fca5a5;
}

.conv-empty {
  padding: 26px 8px;
  color: #526077;
  font-size: 11.5px;
  line-height: 1.9;
  text-align: center;
}

/* -------------------------------- 顶部栏 -------------------------------- */
.chat-header {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 66px;
  padding: 0 22px;
  border-bottom: 1px solid var(--color-border);
  background: #fff;
}

.chat-heading {
  min-width: 0;
}

.chat-heading h1 {
  overflow: hidden;
  color: #1e2235;
  font-size: 15px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-heading p {
  overflow: hidden;
  margin-top: 2px;
  color: #9299ad;
  font-size: 11.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 10px;
}

.mode-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border: 1px solid #e6e3fb;
  border-radius: 20px;
  background: #f8f7ff;
  color: #5b55c4;
  font-size: 11.5px;
  font-weight: 500;
  white-space: nowrap;
}

.mode-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.12);
}

.trace-toggle {
  border: 1px solid var(--color-border);
  background: #fff;
}

/* -------------------------------- 消息区 -------------------------------- */
.chat-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.chat-scroll-inner {
  min-height: 100%;
  padding: 24px 24px 8px;
}

.chat-scroll-inner.is-empty {
  display: flex;
  align-items: center;
}

.chat-scroll-inner.is-empty .empty-state {
  padding-top: 8px;
  padding-bottom: 40px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
  max-width: 820px;
  margin: 0 auto;
}

.message-skeleton {
  max-width: 820px;
  margin: 12px auto;
}

/* -------------------------------- 输入区 -------------------------------- */
.composer {
  flex: 0 0 auto;
  padding: 8px 24px 20px;
  background: linear-gradient(180deg, rgba(247, 248, 252, 0) 0%, var(--color-content) 32%);
}

.composer-box {
  max-width: 820px;
  margin: 0 auto;
  padding: 9px 11px 8px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: #fff;
  box-shadow: 0 6px 22px rgba(15, 23, 42, 0.05);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.composer-box:focus-within {
  border-color: #cfcbf7;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.07), 0 6px 22px rgba(15, 23, 42, 0.05);
}

.composer-box :deep(.el-textarea__inner) {
  padding: 4px 5px 8px;
  border: none;
  background: transparent;
  box-shadow: none;
  color: var(--color-text);
  font-family: var(--font-sans);
  font-size: 14px;
  line-height: 1.7;
}

.composer-box :deep(.el-textarea__inner:focus) {
  box-shadow: none;
}

.composer-box :deep(.el-textarea.is-disabled .el-textarea__inner) {
  background: transparent;
  color: var(--color-text-muted);
}

.composer-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 6px;
  border-top: 1px solid #f3f4f9;
}

.composer-hint {
  display: flex;
  align-items: center;
  gap: 5px;
  overflow: hidden;
  color: #a2a8ba;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.composer-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
}

.composer-actions :deep(.el-button) {
  height: 32px;
  padding: 0 15px;
  border-radius: 9px;
  font-size: 12.5px;
}

.stop-btn {
  border-color: #f2d3d3;
  background: #fff7f7;
  color: #d9534f;
}

.stop-btn:hover {
  border-color: #eab9b9;
  background: #fdf0f0;
  color: #c0392b;
}

.send-icon {
  margin-right: 3px;
}

/* ------------------------------ 折叠后的轨迹 ------------------------------ */
.trace-restore {
  display: flex;
  flex: 0 0 42px;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  width: 42px;
  padding: 18px 0;
  border: none;
  border-left: 1px solid var(--color-border);
  background: #fbfcff;
  color: #8b93a7;
  font-size: 11.5px;
  letter-spacing: 2px;
  cursor: pointer;
  writing-mode: vertical-rl;
  transition: color 0.15s ease, background-color 0.15s ease;
}

.trace-restore:hover {
  background: #f5f4ff;
  color: var(--color-primary);
}

@media (max-width: 900px) {
  .chat-scroll-inner,
  .composer {
    padding-right: 14px;
    padding-left: 14px;
  }

  .chat-header {
    padding: 0 14px;
  }

  .composer-hint {
    display: none;
  }
}
</style>
