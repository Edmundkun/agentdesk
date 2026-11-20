<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  ArrowDown,
  ArrowRight,
  DArrowRight,
  Document,
  Loading,
  MagicStick,
  Operation,
  Search,
} from '@element-plus/icons-vue'
import type { Component } from 'vue'
import type { TraceStep } from '../api/types'

const props = withDefaults(
  defineProps<{
    steps: TraceStep[]
    streaming?: boolean
    durationMs?: number
  }>(),
  { streaming: false, durationMs: 0 },
)

const emit = defineEmits<{ collapse: [] }>()

const expanded = ref<number[]>([])

function isExpanded(index: number): boolean {
  return expanded.value.includes(index)
}

function toggleObservation(index: number): void {
  expanded.value = isExpanded(index)
    ? expanded.value.filter((item) => item !== index)
    : [...expanded.value, index]
}

function toolIcon(tool: string): Component {
  const name = tool.toLowerCase()
  if (name.includes('search') || name.includes('knowledge') || name.includes('retriev')) return Search
  if (name.includes('doc') || name.includes('file')) return Document
  return Operation
}

const durationText = computed<string>(() => {
  if (props.durationMs > 0) return `${props.durationMs} ms`
  return props.streaming ? '进行中' : '—'
})

const hasSteps = computed<boolean>(() => props.steps.length > 0)
</script>

<template>
  <aside class="trace-panel">
    <div class="trace-head">
      <div class="trace-title-row">
        <div class="trace-title">
          <span class="status-dot" :class="{ live: streaming }"></span>
          Agent 运行轨迹
        </div>
        <el-tooltip content="收起轨迹面板" placement="left" :show-after="300">
          <button class="icon-btn" type="button" aria-label="收起轨迹面板" @click="emit('collapse')">
            <el-icon :size="15"><DArrowRight /></el-icon>
          </button>
        </el-tooltip>
      </div>
      <div class="trace-meta">
        <span>第 {{ steps.length }} 轮推理</span>
        <span class="meta-divider"></span>
        <span>耗时 {{ durationText }}</span>
      </div>
    </div>

    <div class="trace-body">
      <template v-if="hasSteps">
        <div v-for="(step, position) in steps" :key="`${step.index}-${position}`" class="trace-step">
          <div class="trace-rail">
            <span class="trace-dot" :class="{ active: streaming && position === steps.length - 1 }">
              <el-icon :size="11"><component :is="toolIcon(step.tool)" /></el-icon>
            </span>
            <span v-if="position < steps.length - 1 || streaming" class="trace-line"></span>
          </div>

          <div class="trace-content">
            <div class="step-head">
              <span class="step-index">STEP {{ step.index }}</span>
              <span class="step-tool">{{ step.tool }}</span>
              <span class="step-latency">{{ step.latencyMs }}ms</span>
            </div>

            <p v-if="step.thought" class="step-thought">{{ step.thought }}</p>

            <div v-if="step.input" class="step-field">
              <span class="field-label">输入</span>
              <code class="field-value">{{ step.input }}</code>
            </div>

            <button type="button" class="observation-toggle" @click="toggleObservation(position)">
              <el-icon :size="11">
                <component :is="isExpanded(position) ? ArrowDown : ArrowRight" />
              </el-icon>
              观察结果
            </button>

            <pre v-if="isExpanded(position)" class="observation">{{ step.observation || '（空）' }}</pre>
          </div>
        </div>

        <div v-if="streaming" class="trace-running">
          <span class="running-dot"></span>
          Agent 正在继续推理…
        </div>
      </template>

      <div v-else class="trace-empty">
        <div class="empty-icon" :class="{ spinning: streaming }">
          <el-icon :size="19">
            <component :is="streaming ? Loading : MagicStick" />
          </el-icon>
        </div>
        <p class="empty-title">{{ streaming ? 'Agent 正在推理' : '暂无推理步骤' }}</p>
        <p class="empty-desc">
          {{
            streaming
              ? '正在检索知识库并组织答案，工具调用步骤即将显示。'
              : '向 Agent 提问后，这里会实时展示每一轮思考、工具调用、输入参数与观察结果。'
          }}
        </p>
        <span class="empty-tag">{{ streaming ? '实时接收中' : 'ReAct 推理过程可视化' }}</span>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.trace-panel {
  display: flex;
  flex: 0 0 320px;
  flex-direction: column;
  width: 320px;
  min-height: 0;
  border-left: 1px solid var(--color-border);
  background: #fbfcff;
}

.trace-head {
  flex: 0 0 auto;
  padding: 16px 16px 12px;
  border-bottom: 1px solid var(--color-border);
  background: #fff;
}

.trace-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.trace-title {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #1e2235;
  font-size: 13.5px;
  font-weight: 600;
  letter-spacing: 0.1px;
}

.status-dot {
  position: relative;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #b6bdd0;
}

.status-dot.live {
  background: #22c55e;
}

.status-dot.live::after {
  position: absolute;
  inset: -3px;
  border: 1px solid rgba(34, 197, 94, 0.4);
  border-radius: 50%;
  content: '';
  animation: status-pulse 1.6s ease-out infinite;
}

@keyframes status-pulse {
  0% {
    opacity: 0.8;
    transform: scale(0.8);
  }
  100% {
    opacity: 0;
    transform: scale(1.5);
  }
}

.trace-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 7px;
  color: #8b93a7;
  font-size: 11.5px;
}

.meta-divider {
  width: 1px;
  height: 10px;
  background: #e2e5ef;
}

.trace-body {
  flex: 1;
  min-height: 0;
  padding: 16px 14px 24px;
  overflow-y: auto;
}

.trace-step {
  display: flex;
  gap: 10px;
}

.trace-rail {
  display: flex;
  flex: 0 0 22px;
  flex-direction: column;
  align-items: center;
  width: 22px;
}

.trace-dot {
  display: flex;
  flex: 0 0 22px;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 1px solid #ddd9fb;
  border-radius: 50%;
  background: #eef2ff;
  color: #4f46e5;
  z-index: 1;
}

.trace-dot.active {
  border-color: #c7c2f7;
  background: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
  color: #fff;
}

.trace-line {
  flex: 1;
  width: 1.5px;
  min-height: 14px;
  margin: 3px 0;
  background: linear-gradient(180deg, #ddd9fb, #eceaf9);
}

.trace-content {
  flex: 1;
  min-width: 0;
  padding-bottom: 16px;
}

.step-head {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 22px;
  margin-bottom: 4px;
}

.step-index {
  color: #a2a8ba;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.5px;
}

.step-tool {
  overflow: hidden;
  padding: 1px 6px;
  border-radius: 5px;
  background: #eef2ff;
  color: #4338ca;
  font-family: var(--font-mono);
  font-size: 10.5px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-latency {
  margin-left: auto;
  color: #a2a8ba;
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.step-thought {
  margin-bottom: 7px;
  color: #3f4557;
  font-size: 12.3px;
  line-height: 1.65;
  white-space: pre-wrap;
}

.step-field {
  margin-bottom: 5px;
}

.field-label {
  display: block;
  margin-bottom: 2px;
  color: #a2a8ba;
  font-size: 10px;
  letter-spacing: 0.3px;
}

.field-value {
  display: block;
  overflow: auto;
  max-height: 76px;
  padding: 6px 8px;
  border: 1px solid #eef0f6;
  border-radius: 7px;
  background: #f7f8fc;
  color: #4b5063;
  font-family: var(--font-mono);
  font-size: 11px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.observation-toggle {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 0;
  border: none;
  background: transparent;
  color: #7c849a;
  font-size: 11px;
  cursor: pointer;
}

.observation-toggle:hover {
  color: var(--color-primary);
}

.observation {
  overflow: auto;
  max-height: 220px;
  margin: 5px 0 0;
  padding: 8px 10px;
  border: 1px solid #1e293b;
  border-radius: 8px;
  background: #0f172a;
  color: #cbd5e1;
  font-family: var(--font-mono);
  font-size: 10.8px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.trace-running {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 4px 0 0 32px;
  color: #8b93a7;
  font-size: 11.5px;
}

.running-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #8b87e8;
  animation: running 1.2s ease-in-out infinite;
}

@keyframes running {
  0%,
  100% {
    opacity: 0.35;
    transform: scale(0.85);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
}

.trace-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 44px 16px;
  text-align: center;
}

.empty-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  margin-bottom: 12px;
  border: 1px solid #e3e0fb;
  border-radius: 12px;
  background: #f5f4ff;
  color: #6d68c9;
}

.empty-icon.spinning .el-icon {
  animation: icon-spin 1.4s linear infinite;
}

@keyframes icon-spin {
  to {
    transform: rotate(360deg);
  }
}

.empty-title {
  color: #3f4557;
  font-size: 13px;
  font-weight: 600;
}

.empty-desc {
  margin-top: 5px;
  color: #9299ad;
  font-size: 11.5px;
  line-height: 1.7;
}

.empty-tag {
  margin-top: 14px;
  padding: 3px 8px;
  border: 1px solid #eceaf9;
  border-radius: 20px;
  background: #fff;
  color: #a2a8ba;
  font-size: 10px;
  letter-spacing: 0.2px;
}

@media (max-width: 1180px) {
  .trace-panel {
    flex-basis: 288px;
    width: 288px;
  }
}
</style>
