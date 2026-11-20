# AgentDesk 前端

企业知识库智能体工作台的前端实现：Vue 3.5 `<script setup>` + TypeScript + Vite 5 + Pinia + Element Plus。
核心亮点是「Agent 运行轨迹」面板——实时展示 ReAct 智能体每一轮思考、工具调用与观察结果。

## 开发

```bash
npm install
npm run dev      # http://localhost:5173，/api 已代理到 http://localhost:8080
```

`npm run build` 会先执行 `vue-tsc --noEmit` 类型检查，再产出 `dist/`。

## 环境变量

| 变量 | 说明 | 默认 |
| --- | --- | --- |
| `VITE_LLM_MODE` | 对话模式标签，`mock` 显示「本地规则模型」，`llm` 显示「LLM」 | `mock` |

## 目录

- `src/api`：axios 实例与拦截器、认证 / 会话 / 知识库接口、SSE 流式解析
- `src/stores`：Pinia（登录态、会话与推理轨迹状态）
- `src/views`：登录、三栏对话工作台、知识库
- `src/components`：AppShell、消息气泡、轨迹面板、Markdown 渲染、空状态
- `src/styles/main.css`：设计变量与全局样式

演示账号：`demo / 123456`。
