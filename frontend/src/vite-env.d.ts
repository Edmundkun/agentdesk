/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Agent 推理模式：mock（本地规则模型）| llm（大模型） */
  readonly VITE_LLM_MODE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
