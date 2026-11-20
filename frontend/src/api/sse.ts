import type { StreamDone, StreamMeta, TraceStep } from './types'

export interface StreamHandlers {
  onMeta?: (meta: StreamMeta) => void
  onStep?: (step: TraceStep) => void
  onToken?: (text: string) => void
  onDone?: (payload: StreamDone) => void
  onError?: (message: string) => void
}

export interface SseFrame {
  event: string
  data: string
}

export interface SseDecoder {
  /** 追加一段解码后的文本，返回本次已经完整的帧 */
  push(text: string): SseFrame[]
  /** 流结束：冲刷缓冲区里最后一条没有空行结尾的帧 */
  end(text?: string): SseFrame[]
}

/**
 * 增量式 SSE 解码器。
 * - 按空行切分帧，兼容 `\r\n` 与 `\n`；
 * - 未闭合的半截帧留在 buffer 中，等待下一个 chunk（含被切断的 `\r\n`）。
 */
export function createSseDecoder(): SseDecoder {
  let buffer = ''

  function takeFrames(flush: boolean): SseFrame[] {
    const frames: SseFrame[] = []
    let boundary = buffer.indexOf('\n\n')

    while (boundary !== -1) {
      const raw = buffer.slice(0, boundary)
      buffer = buffer.slice(boundary + 2)
      const frame = parseFrame(raw)
      if (frame) frames.push(frame)
      boundary = buffer.indexOf('\n\n')
    }

    if (flush) {
      const tail = buffer.trim()
      buffer = ''
      if (tail) {
        const frame = parseFrame(tail)
        if (frame) frames.push(frame)
      }
    }
    return frames
  }

  return {
    push(text: string): SseFrame[] {
      buffer = (buffer + text).replace(/\r\n/g, '\n')
      return takeFrames(false)
    },
    end(text = ''): SseFrame[] {
      buffer = (buffer + text).replace(/\r\n/g, '\n')
      return takeFrames(true)
    },
  }
}

function parseFrame(raw: string): SseFrame | null {
  if (!raw.trim()) return null

  let event = 'message'
  const dataLines: string[] = []

  for (const line of raw.split('\n')) {
    if (!line || line.startsWith(':')) continue
    const colon = line.indexOf(':')
    const field = colon === -1 ? line : line.slice(0, colon)
    let value = colon === -1 ? '' : line.slice(colon + 1)
    if (value.startsWith(' ')) value = value.slice(1)

    if (field === 'event') event = value.trim()
    else if (field === 'data') dataLines.push(value)
  }

  if (dataLines.length === 0) return null
  const data = dataLines.join('\n').trim()
  if (!data) return null
  return { event, data }
}

/** 把一帧分发到回调，返回 true 表示收到终止事件（done / error） */
export function dispatchSseFrame(frame: SseFrame, handlers: StreamHandlers): boolean {
  const { data: raw, event } = frame
  if (raw === '[DONE]') return true

  const data = parseObject(raw)
  if (!data) return false

  switch (event) {
    case 'meta':
      handlers.onMeta?.({
        conversationId: pickNumber(data.conversationId),
        assistantMessageId: pickNumber(data.assistantMessageId),
      })
      return false
    case 'step':
      handlers.onStep?.({
        index: pickNumber(data.index),
        thought: pickString(data.thought),
        tool: pickString(data.tool),
        input: pickString(data.input),
        observation: pickString(data.observation),
        latencyMs: pickNumber(data.latencyMs),
      })
      return false
    case 'token':
      handlers.onToken?.(pickString(data.text))
      return false
    case 'done':
      handlers.onDone?.({
        messageId: pickNumber(data.messageId),
        answer: pickString(data.answer),
        durationMs: pickNumber(data.durationMs),
        stepCount: pickNumber(data.stepCount),
      })
      return true
    case 'error':
      handlers.onError?.(pickString(data.message) || 'Agent 运行出错')
      return true
    default:
      return false
  }
}

function parseObject(raw: string): Record<string, unknown> | null {
  try {
    const value: unknown = JSON.parse(raw)
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      return value as Record<string, unknown>
    }
  } catch {
    /* 忽略无法解析的帧，避免整条流中断 */
  }
  return null
}

function pickString(value: unknown, fallback = ''): string {
  return typeof value === 'string' ? value : fallback
}

function pickNumber(value: unknown, fallback = 0): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback
}
