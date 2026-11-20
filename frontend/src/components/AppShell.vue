<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Collection, SwitchButton } from '@element-plus/icons-vue'
import type { Component } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useChatStore } from '../stores/chat'

interface NavItem {
  path: string
  label: string
  icon: Component
}

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const chat = useChatStore()

const navItems: NavItem[] = [
  { path: '/chat', label: '对话工作台', icon: ChatDotRound },
  { path: '/knowledge', label: '知识库', icon: Collection },
]

const activePath = computed<string>(() => (route.path.startsWith('/knowledge') ? '/knowledge' : '/chat'))

async function handleLogout(): Promise<void> {
  try {
    await ElMessageBox.confirm('退出后需要重新登录，确定退出吗？', '退出登录', {
      confirmButtonText: '退出登录',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  chat.reset()
  auth.logout()
  ElMessage.success('已退出登录')
  await router.replace('/login')
}
</script>

<template>
  <div class="app-shell">
    <aside class="shell-sidebar">
      <div class="shell-brand">
        <div class="brand-mark">A</div>
        <div class="brand-text">
          <span class="brand-name">AgentDesk</span>
          <span class="brand-sub">企业知识库智能体</span>
        </div>
      </div>

      <nav class="shell-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ 'is-active': activePath === item.path }"
        >
          <el-icon :size="16"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="shell-slot">
        <slot name="sidebar" />
      </div>

      <div class="shell-footer">
        <div class="avatar">{{ auth.avatarText }}</div>
        <div class="user-meta">
          <span class="user-name">{{ auth.displayName }}</span>
          <span class="user-sub">@{{ auth.user?.username ?? 'guest' }}</span>
        </div>
        <el-tooltip content="退出登录" placement="top" :show-after="300">
          <button class="icon-btn dark" type="button" aria-label="退出登录" @click="handleLogout">
            <el-icon :size="16"><SwitchButton /></el-icon>
          </button>
        </el-tooltip>
      </div>
    </aside>

    <main class="shell-main">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: var(--color-content);
}

.shell-sidebar {
  display: flex;
  flex: 0 0 260px;
  flex-direction: column;
  width: 260px;
  min-height: 0;
  color: #cbd5e1;
  background: var(--color-sidebar);
  border-right: 1px solid rgba(148, 163, 184, 0.08);
}

.shell-brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 20px 18px 18px;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #4338ca);
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.36);
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
}

.brand-name {
  color: #f1f5f9;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.brand-sub {
  color: #64748b;
  font-size: 11px;
}

.shell-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 2px 10px 12px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 11px;
  border-radius: 9px;
  color: #94a3b8;
  font-size: 13.5px;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.nav-item:hover {
  background: rgba(148, 163, 184, 0.08);
  color: #e2e8f0;
}

.nav-item.is-active {
  background: rgba(99, 102, 241, 0.18);
  color: #e5e7ff;
}

.shell-slot {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding: 0 10px;
  overflow: hidden;
}

.shell-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px 14px;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
}

.avatar {
  display: flex;
  flex: 0 0 32px;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: rgba(99, 102, 241, 0.22);
  color: #c7d2fe;
  font-size: 13px;
  font-weight: 600;
}

.user-meta {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  line-height: 1.35;
}

.user-name {
  overflow: hidden;
  color: #e2e8f0;
  font-size: 13px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-sub {
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.icon-btn.dark:hover {
  background: rgba(148, 163, 184, 0.12);
  color: #e2e8f0;
}

.shell-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

@media (max-width: 900px) {
  .shell-sidebar {
    flex-basis: 220px;
    width: 220px;
  }
}
</style>
