<script setup lang="ts">
import type { Component } from 'vue'
import { ArrowRight } from '@element-plus/icons-vue'

export interface ExampleItem {
  icon: Component
  title: string
  question: string
}

withDefaults(
  defineProps<{
    title?: string
    description?: string
    examples?: ExampleItem[]
  }>(),
  {
    title: '你好，我是 AgentDesk',
    description: '基于企业知识库的 ReAct 智能体。上传文档后向我提问，我会边检索边推理，并把每一步都展示给你。',
    examples: () => [],
  },
)

const emit = defineEmits<{ select: [question: string] }>()
</script>

<template>
  <div class="empty-state">
    <div class="empty-mark">
      <span class="mark-core">A</span>
      <span class="mark-ring"></span>
    </div>

    <h2 class="empty-title">{{ title }}</h2>
    <p class="empty-desc">{{ description }}</p>

    <div v-if="examples.length" class="example-grid">
      <button
        v-for="item in examples"
        :key="item.question"
        type="button"
        class="example-card"
        @click="emit('select', item.question)"
      >
        <span class="example-icon">
          <el-icon :size="15"><component :is="item.icon" /></el-icon>
        </span>
        <span class="example-title">{{ item.title }}</span>
        <span class="example-question">{{ item.question }}</span>
        <el-icon class="example-arrow" :size="13"><ArrowRight /></el-icon>
      </button>
    </div>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 780px;
  margin: 0 auto;
  padding: 48px 16px 24px;
  text-align: center;
}

.empty-mark {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  margin-bottom: 18px;
}

.mark-core {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 14px;
  background: linear-gradient(140deg, #6366f1, #4338ca);
  box-shadow: 0 10px 24px rgba(79, 70, 229, 0.26);
  color: #fff;
  font-size: 21px;
  font-weight: 700;
}

.mark-ring {
  position: absolute;
  inset: 0;
  border: 1px solid rgba(99, 102, 241, 0.24);
  border-radius: 17px;
  transform: rotate(8deg);
}

.empty-title {
  color: #171b2c;
  font-size: 21px;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.empty-desc {
  max-width: 520px;
  margin-top: 8px;
  color: #7c849a;
  font-size: 13px;
  line-height: 1.75;
}

.example-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
  margin-top: 30px;
}

.example-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 14px 15px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.example-card:hover {
  border-color: #cfcbf7;
  box-shadow: 0 8px 22px rgba(79, 70, 229, 0.08);
  transform: translateY(-1px);
}

.example-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  margin-bottom: 2px;
  border-radius: 8px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.example-title {
  color: #262b3d;
  font-size: 13px;
  font-weight: 600;
}

.example-question {
  color: #8b93a7;
  font-size: 11.5px;
  line-height: 1.6;
}

.example-arrow {
  position: absolute;
  right: 12px;
  bottom: 12px;
  color: #c3c8d6;
  opacity: 0;
  transition: opacity 0.15s ease, color 0.15s ease;
}

.example-card:hover .example-arrow {
  color: var(--color-primary);
  opacity: 1;
}

@media (max-width: 780px) {
  .example-grid {
    grid-template-columns: 1fr;
  }
}
</style>
