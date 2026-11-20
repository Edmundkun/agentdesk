/**
 * 与 docs/api.md（v1 契约）一一对应的类型定义。
 * 所有接口统一响应体：{ code, message, data }
 */

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/* ------------------------------- Auth ------------------------------- */

export interface User {
  id: number
  username: string
  nickname: string
  createdAt?: string
}

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  username: string
  password: string
  nickname: string
}

export interface AuthResult {
  token: string
  user: User
}

/* ----------------------------- Knowledge ---------------------------- */

export interface KnowledgeStats {
  documentCount: number
  chunkCount: number
  totalChars: number
}

export interface KnowledgeDocument {
  id: number
  name: string
  fileType: string
  sizeBytes: number
  chunkCount: number
  status: string
  createdAt: string
}

export interface SearchHit {
  documentId: number
  documentName: string
  chunkIndex: number
  content: string
  score: number
}

/* -------------------------------- Chat ------------------------------ */

export interface Conversation {
  id: number
  title: string
  createdAt: string
  updatedAt: string
  messageCount: number
}

export type ChatRole = 'user' | 'assistant'

/** 一轮工具调用的推理步骤 */
export interface TraceStep {
  index: number
  thought: string
  tool: string
  input: string
  observation: string
  latencyMs: number
}

export interface ChatMessage {
  id: number | string
  role: ChatRole
  content: string
  trace: TraceStep[] | null
  createdAt: string
  /** 前端本地标记：该条消息是否仍在流式接收 */
  streaming?: boolean
}

/* ------------------------------ SSE 事件 ----------------------------- */

export interface StreamMeta {
  conversationId: number
  assistantMessageId: number
}

export interface StreamDone {
  messageId: number
  answer: string
  durationMs: number
  stepCount: number
}
