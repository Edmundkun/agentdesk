<script setup lang="ts">
import { computed, nextTick, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Collection, Lightning, Lock, MagicStick, User } from '@element-plus/icons-vue'
import type { Component } from 'vue'
import { useAuthStore } from '../stores/auth'

interface AuthForm {
  username: string
  password: string
  nickname: string
}

interface SellingPoint {
  icon: Component
  title: string
  desc: string
}

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const isRegister = ref(false)
const form = reactive<AuthForm>({ username: '', password: '', nickname: '' })

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 32, message: '用户名长度为 4~32 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6~32 位', trigger: 'blur' },
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 32, message: '昵称不超过 32 位', trigger: 'blur' },
  ],
}

const sellingPoints: SellingPoint[] = [
  {
    icon: MagicStick,
    title: 'ReAct 推理轨迹可视化',
    desc: '每一步思考、工具调用与观察结果实时呈现，答案有迹可循。',
  },
  {
    icon: Collection,
    title: '企业知识库检索增强',
    desc: '上传制度、手册与文档，Agent 先检索再作答，避免凭空编造。',
  },
  {
    icon: Lightning,
    title: '流式输出与多会话管理',
    desc: '答案逐字返回，随时中断生成，历史会话沉淀可追溯。',
  },
]

const previewSteps = [
  { tool: 'knowledge_search', thought: '先检索报销制度中与“提交时限”相关的条款', latency: '18ms' },
  { tool: 'knowledge_search', thought: '补充检索“差旅”关键词，确认材料清单', latency: '12ms' },
]

const title = computed<string>(() => (isRegister.value ? '创建账号' : '欢迎回来'))
const subtitle = computed<string>(() =>
  isRegister.value ? '注册后即可上传文档并开始对话' : '登录 AgentDesk，继续你的知识对话',
)
const submitText = computed<string>(() => (isRegister.value ? '注册并进入' : '登录'))

function switchMode(register: boolean): void {
  if (isRegister.value === register) return
  isRegister.value = register
  void nextTick(() => formRef.value?.clearValidate())
}

function fillDemo(): void {
  isRegister.value = false
  form.username = 'demo'
  form.password = '123456'
  void nextTick(() => formRef.value?.clearValidate())
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value || auth.loading) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (isRegister.value) {
      await auth.register({
        username: form.username.trim(),
        password: form.password,
        nickname: form.nickname.trim(),
      })
      ElMessage.success('注册成功，已为你自动登录')
    } else {
      await auth.login({ username: form.username.trim(), password: form.password })
      ElMessage.success(`欢迎回来，${auth.displayName}`)
    }
    const redirect =
      typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
        ? route.query.redirect
        : '/chat'
    await router.replace(redirect)
  } catch {
    /* 拦截器已统一提示错误 */
  }
}
</script>

<template>
  <div class="login-page">
    <section class="brand-panel">
      <div class="deco-grid"></div>
      <div class="deco-glow one"></div>
      <div class="deco-glow two"></div>

      <header class="brand-top">
        <div class="brand-logo">
          <span class="logo-mark">A</span>
          <span class="logo-name">AgentDesk</span>
        </div>
        <span class="brand-badge">ReAct Agent</span>
      </header>

      <div class="brand-copy">
        <p class="brand-eyebrow">企业知识库智能体工作台</p>
        <h1>让企业知识<br /><span>可检索，更可解释</span></h1>
        <p class="brand-tagline">
          上传文档，用自然语言提问。AgentDesk 会先检索企业知识库，再逐步推理作答，并把完整的思考轨迹展示给你。
        </p>

        <ul class="selling-points">
          <li v-for="point in sellingPoints" :key="point.title" class="point">
            <span class="point-icon">
              <el-icon :size="15"><component :is="point.icon" /></el-icon>
            </span>
            <div class="point-body">
              <p class="point-title">{{ point.title }}</p>
              <p class="point-desc">{{ point.desc }}</p>
            </div>
          </li>
        </ul>
      </div>

      <div class="brand-preview">
        <div class="preview-head">
          <span class="preview-title">
            <span class="preview-dot"></span>
            Agent 运行轨迹
          </span>
          <span class="preview-meta">第 2 轮推理 · 耗时 420ms</span>
        </div>
        <div v-for="(step, index) in previewSteps" :key="step.tool + index" class="preview-step">
          <div class="preview-rail">
            <span class="preview-node"></span>
            <span v-if="index < previewSteps.length - 1" class="preview-line"></span>
          </div>
          <div class="preview-content">
            <div class="preview-step-head">
              <code>{{ step.tool }}</code>
              <span>{{ step.latency }}</span>
            </div>
            <p>{{ step.thought }}</p>
          </div>
        </div>
      </div>

      <footer class="brand-foot">© 2026 AgentDesk · 企业知识库智能体演示项目</footer>
    </section>

    <section class="form-panel">
      <div class="form-card">
        <div class="mobile-brand">
          <span class="logo-mark">A</span>
          <span class="logo-name">AgentDesk</span>
        </div>

        <div class="form-head">
          <h2>{{ title }}</h2>
          <p>{{ subtitle }}</p>
        </div>

        <div class="mode-switch" role="tablist">
          <button
            type="button"
            role="tab"
            :aria-selected="!isRegister"
            :class="{ active: !isRegister }"
            @click="switchMode(false)"
          >
            登录
          </button>
          <button
            type="button"
            role="tab"
            :aria-selected="isRegister"
            :class="{ active: isRegister }"
            @click="switchMode(true)"
          >
            注册
          </button>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          class="auth-form"
          @submit.prevent="handleSubmit"
        >
          <el-form-item v-if="isRegister" label="昵称" prop="nickname">
            <el-input
              v-model="form.nickname"
              placeholder="展示在侧边栏的名字，1~32 位"
              maxlength="32"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="4~32 位，字母 / 数字 / 下划线"
              maxlength="32"
              autocomplete="username"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="6~32 位"
              maxlength="32"
              autocomplete="current-password"
              :prefix-icon="Lock"
              @keyup.enter="handleSubmit"
            />
          </el-form-item>

          <el-button
            class="submit-btn"
            type="primary"
            :loading="auth.loading"
            @click="handleSubmit"
          >
            {{ submitText }}
          </el-button>
        </el-form>

        <div class="demo-row">
          <span class="demo-label">演示账号</span>
          <button type="button" class="demo-btn" @click="fillDemo">demo / 123456</button>
        </div>

        <p class="form-note">
          登录即表示你了解：本项目为简历演示用途，数据仅用于功能展示。
        </p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: #fff;
}

/* -------------------------------- 品牌区 -------------------------------- */
.brand-panel {
  position: relative;
  display: flex;
  flex: 1 1 56%;
  flex-direction: column;
  justify-content: space-between;
  gap: 34px;
  overflow: hidden;
  padding: 40px 56px 34px;
  background:
    radial-gradient(900px 520px at 12% 0%, rgba(79, 70, 229, 0.28), transparent 62%),
    linear-gradient(158deg, #0b1120 0%, #141a33 46%, #1d1c44 100%);
  color: #e2e8f0;
}

.deco-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.075) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.075) 1px, transparent 1px);
  background-size: 46px 46px;
  pointer-events: none;
  mask-image: radial-gradient(ellipse at 30% 18%, #000 8%, transparent 72%);
  -webkit-mask-image: radial-gradient(ellipse at 30% 18%, #000 8%, transparent 72%);
}

.deco-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
}

.deco-glow.one {
  top: -140px;
  right: -110px;
  width: 420px;
  height: 420px;
  background: rgba(79, 70, 229, 0.5);
  opacity: 0.55;
}

.deco-glow.two {
  bottom: -150px;
  left: -100px;
  width: 360px;
  height: 360px;
  background: rgba(14, 165, 233, 0.28);
  opacity: 0.35;
}

.brand-top,
.brand-copy,
.brand-preview,
.brand-foot {
  position: relative;
  z-index: 1;
}

.brand-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand-logo,
.mobile-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #818cf8, #4f46e5);
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.4);
  color: #fff;
  font-size: 17px;
  font-weight: 700;
}

.logo-name {
  color: #f8fafc;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.brand-badge {
  padding: 4px 10px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 20px;
  background: rgba(15, 23, 42, 0.4);
  color: #a5b4fc;
  font-size: 11px;
  letter-spacing: 0.4px;
}

.brand-copy {
  max-width: 540px;
}

.brand-eyebrow {
  margin-bottom: 14px;
  color: #818cf8;
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 2.4px;
}

.brand-copy h1 {
  color: #f8fafc;
  font-size: clamp(30px, 3.2vw, 42px);
  font-weight: 600;
  line-height: 1.28;
  letter-spacing: 0.5px;
}

.brand-copy h1 span {
  background: linear-gradient(96deg, #a5b4fc, #7dd3fc);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.brand-tagline {
  max-width: 500px;
  margin-top: 18px;
  color: #94a3b8;
  font-size: 13px;
  line-height: 1.85;
}

.selling-points {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-top: 34px;
}

.point {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.point-icon {
  display: flex;
  flex: 0 0 32px;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid rgba(129, 140, 248, 0.28);
  border-radius: 9px;
  background: rgba(79, 70, 229, 0.16);
  color: #a5b4fc;
}

.point-title {
  color: #e2e8f0;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.point-desc {
  margin-top: 2px;
  color: #8b95ab;
  font-size: 11.8px;
  line-height: 1.7;
}

/* 轨迹预览卡片 */
.brand-preview {
  max-width: 480px;
  padding: 14px 16px 10px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.46);
  box-shadow: 0 18px 44px rgba(2, 6, 23, 0.34);
  backdrop-filter: blur(6px);
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 9px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}

.preview-title {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #e2e8f0;
  font-size: 11.5px;
  font-weight: 500;
}

.preview-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.14);
}

.preview-meta {
  color: #7c8aa5;
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.preview-step {
  display: flex;
  gap: 9px;
  padding-top: 10px;
}

.preview-rail {
  display: flex;
  flex: 0 0 14px;
  flex-direction: column;
  align-items: center;
  width: 14px;
}

.preview-node {
  width: 8px;
  height: 8px;
  margin-top: 5px;
  border: 1px solid rgba(165, 180, 252, 0.5);
  border-radius: 50%;
  background: rgba(99, 102, 241, 0.4);
}

.preview-line {
  flex: 1;
  width: 1px;
  min-height: 10px;
  margin-top: 3px;
  background: rgba(148, 163, 184, 0.18);
}

.preview-step-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-step-head code {
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(99, 102, 241, 0.18);
  color: #c7d2fe;
  font-family: var(--font-mono);
  font-size: 10px;
}

.preview-step-head span {
  color: #64748b;
  font-size: 9.5px;
}

.preview-content p {
  margin-top: 3px;
  color: #94a3b8;
  font-size: 11px;
  line-height: 1.6;
}

.brand-foot {
  color: #526077;
  font-size: 10.5px;
  letter-spacing: 0.2px;
}

/* -------------------------------- 表单区 -------------------------------- */
.form-panel {
  display: flex;
  flex: 1 1 44%;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  background: #fff;
}

.form-card {
  width: 100%;
  max-width: 376px;
}

.mobile-brand {
  display: none;
  margin-bottom: 26px;
}

.mobile-brand .logo-name {
  color: #1e2235;
}

.form-head h2 {
  color: #171b2c;
  font-size: 23px;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.form-head p {
  margin-top: 6px;
  color: #8b93a7;
  font-size: 12.5px;
}

.mode-switch {
  display: flex;
  gap: 3px;
  margin: 26px 0 22px;
  padding: 3px;
  border: 1px solid #eef0f6;
  border-radius: 10px;
  background: #f6f7fb;
}

.mode-switch button {
  flex: 1;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #7c849a;
  font-size: 12.8px;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, box-shadow 0.15s ease;
}

.mode-switch button.active {
  background: #fff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
  color: #1e2235;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.auth-form :deep(.el-form-item__label) {
  padding-bottom: 5px;
  color: #4b5563;
  font-size: 12.5px;
  font-weight: 500;
  line-height: 1.4;
}

.auth-form :deep(.el-input__wrapper) {
  padding: 2px 12px;
  border-radius: 9px;
  background: #fbfbfe;
  box-shadow: 0 0 0 1px #e8eaf2 inset;
}

.auth-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #d7d9e6 inset;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px #a5a0f2 inset, 0 0 0 3px rgba(79, 70, 229, 0.08);
}

.auth-form :deep(.el-input__inner) {
  height: 38px;
  font-size: 13.5px;
}

.auth-form :deep(.el-input__prefix) {
  color: #a2a8ba;
}

.submit-btn {
  width: 100%;
  height: 42px;
  margin-top: 6px;
  border-radius: 10px;
  font-size: 13.5px;
  font-weight: 500;
  letter-spacing: 0.4px;
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.2);
}

.demo-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 20px;
  font-size: 11.5px;
}

.demo-label {
  color: #a2a8ba;
}

.demo-btn {
  padding: 3px 9px;
  border: 1px dashed #d9d5f7;
  border-radius: 6px;
  background: #f9f8ff;
  color: #5b55c4;
  font-family: var(--font-mono);
  font-size: 11px;
  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease;
}

.demo-btn:hover {
  border-color: #b7b1f0;
  color: #4338ca;
}

.form-note {
  margin-top: 26px;
  color: #b3b9c9;
  font-size: 10.5px;
  line-height: 1.7;
  text-align: center;
}

@media (max-width: 1024px) {
  .brand-panel {
    padding: 34px 36px 28px;
  }

  .brand-preview {
    display: none;
  }
}

@media (max-width: 860px) {
  .brand-panel {
    display: none;
  }

  .form-panel {
    padding: 40px 24px;
  }

  .mobile-brand {
    display: flex;
  }
}

@media (max-height: 780px) {
  .brand-preview {
    display: none;
  }
}
</style>
