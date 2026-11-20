<script setup lang="ts">
import { computed } from 'vue'
import { Cpu, Tools } from '@element-plus/icons-vue'
import type { ChatMessage } from '../api/types'
import MarkdownView from './MarkdownView.vue'

const props = defineProps<{ message: ChatMessage }>()

const isUser = computed<boolean>(() => props.message.role === 'user')

const traceSummary = computed<string>(() => {
  const trace = props.message.trace ?? []
  const tools = [...new Set(trace.map((step) => step.tool).filter(Boolean))]
  return tools.join(' · ')
})
</script>

<template>
  <div class="message-row" :class="isUser ? 'is-user' : 'is-assistant'">
    <div v-if="!isUser" class="assistant-avatar">
      <el-icon :size="17"><Cpu /></el-icon>
    </div>

    <div class="message-body">
      <div v-if="!isUser" class="message-author">
        <span>AgentDesk</span>
        <span class="role-chip">ReAct Agent</span>
      </div>

      <div class="bubble" :class="isUser ? 'bubble-user' : 'bubble-assistant'">
        <p v-if="isUser" class="user-text">{{ message.content }}</p>
        <template v-else>
          <MarkdownView v-if="message.content" :content="message.content" />
          <div v-else class="thinking">
            <span class="thinking-dots">
              <i></i><i></i><i></i>
            </span>
            <span>正在分析问题并检索知识库…</span>
          </div>
          <span v-if="message.streaming && message.content" class="stream-caret" aria-hidden="true"></span>
        </template>
      </div>

      <div v-if="!isUser && message.trace && message.trace.length > 0" class="message-foot">
        <el-icon :size="13"><Tools /></el-icon>
        <span>已完成 {{ message.trace.length }} 轮工具调用</span>
        <template v-if="traceSummary">
          <span class="sep">·</span>
          <span class="foot-tools">{{ traceSummary }}</span>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.message-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.message-row.is-user {
  justify-content: flex-end;
}

.assistant-avatar {
  display: flex;
  flex: 0 0 32px;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  margin-top: 22px;
  border-radius: 9px;
  background: linear-gradient(135deg, #6366f1, #4338ca);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.24);
  color: #fff;
}

.message-body {
  min-width: 0;
  max-width: min(720px, 86%);
}

.is-user .message-body {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-author {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  color: #6b7280;
  font-size: 12px;
  font-weight: 500;
}

.role-chip {
  padding: 1px 6px;
  border: 1px solid #e0e2f5;
  border-radius: 5px;
  background: #f7f7ff;
  color: #6d68c9;
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.2px;
}

.bubble {
  position: relative;
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 14.5px;
  line-height: 1.75;
}

.bubble-user {
  max-width: 100%;
  border: 1px solid #dcd9fb;
  border-top-right-radius: 5px;
  background: #f0efff;
  color: #2b2a4a;
}

.user-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-assistant {
  border: 1px solid var(--color-border);
  border-top-left-radius: 5px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
}

.thinking {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 0;
  color: #8b93a7;
  font-size: 13px;
}

.thinking-dots {
  display: inline-flex;
  gap: 3px;
}

.thinking-dots i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #a5a0f2;
  animation: dot-pulse 1.1s ease-in-out infinite;
}

.thinking-dots i:nth-child(2) {
  animation-delay: 0.15s;
}

.thinking-dots i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes dot-pulse {
  0%,
  100% {
    opacity: 0.35;
  }
  50% {
    opacity: 1;
  }
}

.stream-caret {
  display: inline-block;
  width: 2px;
  height: 15px;
  margin-left: 2px;
  vertical-align: -2px;
  background: var(--color-primary);
  animation: caret-blink 1s steps(2, start) infinite;
}

@keyframes caret-blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

.message-foot {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 7px;
  padding-left: 2px;
  color: #9299ad;
  font-size: 11.5px;
}

.foot-tools {
  overflow: hidden;
  max-width: 320px;
  color: #7a80a0;
  font-family: var(--font-mono);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sep {
  color: #c3c8d6;
}

@media (max-width: 720px) {
  .message-body {
    max-width: 88%;
  }
}
</style>
