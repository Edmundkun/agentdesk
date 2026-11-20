# AgentDesk 接口契约（v1）

- Base URL：`http://localhost:8080/api`
- 除 `/auth/login`、`/auth/register` 外，所有接口都需要请求头：`Authorization: Bearer <token>`
- 统一响应体：

```json
{ "code": 0, "message": "ok", "data": {} }
```

`code = 0` 表示成功；非 0 时 HTTP 状态码为 400 / 401 / 403 / 404 / 500，`message` 为可直接展示的中文提示。

## 1. 认证 Auth

### POST /auth/register
```json
{ "username": "alice", "password": "123456", "nickname": "Alice" }
```
校验：username 4~32 位、password 6~32 位、nickname 1~32 位。

### POST /auth/login
```json
{ "username": "demo", "password": "123456" }
```

两者返回一致：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": { "id": 1, "username": "demo", "nickname": "演示用户" }
  }
}
```

### GET /auth/me
```json
{ "code": 0, "message": "ok", "data": { "id": 1, "username": "demo", "nickname": "演示用户", "createdAt": "2026-01-01 10:00:00" } }
```

## 2. 知识库 Knowledge

### GET /knowledge/stats
```json
{ "code": 0, "message": "ok", "data": { "documentCount": 3, "chunkCount": 12, "totalChars": 5231 } }
```

### GET /knowledge/documents
```json
{
  "code": 0, "message": "ok",
  "data": [
    {
      "id": 1,
      "name": "员工报销制度.md",
      "fileType": "md",
      "sizeBytes": 3120,
      "chunkCount": 4,
      "status": "READY",
      "createdAt": "2026-01-01 10:00:00"
    }
  ]
}
```

### POST /knowledge/documents
`multipart/form-data`，字段名 `file`，支持 `.txt` / `.md` / `.pdf`，单文件 ≤ 10MB。
返回单个 DocumentVO（同上）。

### DELETE /knowledge/documents/{id}
`{ "code": 0, "message": "ok", "data": null }`

### POST /knowledge/search
```json
{ "query": "差旅报销多久内提交", "topK": 4 }
```
```json
{
  "code": 0, "message": "ok",
  "data": [
    {
      "documentId": 1,
      "documentName": "员工报销制度.md",
      "chunkIndex": 2,
      "content": "差旅报销需在出差结束后 10 个工作日内提交……",
      "score": 0.8234
    }
  ]
}
```

## 3. 会话与对话 Chat

### GET /chat/conversations
```json
{
  "code": 0, "message": "ok",
  "data": [
    { "id": 1, "title": "差旅报销多久内提交？", "createdAt": "2026-01-01 10:00:00", "updatedAt": "2026-01-01 10:01:00", "messageCount": 2 }
  ]
}
```

### POST /chat/conversations
请求体可省略：`{ "title": "新会话" }`，返回同上单个对象。

### DELETE /chat/conversations/{id}
返回 `data: null`。

### GET /chat/conversations/{id}/messages
```json
{
  "code": 0, "message": "ok",
  "data": [
    { "id": 1, "role": "user", "content": "差旅报销多久内提交？", "trace": null, "createdAt": "2026-01-01 10:00:00" },
    {
      "id": 2, "role": "assistant", "content": "根据《员工报销制度》……",
      "trace": [
        {
          "index": 1,
          "thought": "需要检索企业知识库",
          "tool": "knowledge_search",
          "input": "差旅报销多久内提交",
          "observation": "[1] 员工报销制度.md#2 (score=0.82)\n差旅报销需在……",
          "latencyMs": 18
        }
      ],
      "createdAt": "2026-01-01 10:00:03"
    }
  ]
}
```

### POST /chat/stream  （SSE，`Accept: text/event-stream`）
请求：
```json
{ "conversationId": 1, "question": "差旅报销多久内提交？" }
```
`conversationId` 可为 `null`，后端会自动创建会话。响应事件流：

| event | data | 说明 |
|---|---|---|
| `meta` | `{"conversationId":1,"assistantMessageId":9}` | 首个事件，告知新会话 id |
| `step` | `{"index":1,"thought":"...","tool":"knowledge_search","input":"...","observation":"...","latencyMs":18}` | 每完成一轮工具调用推送一次 |
| `token` | `{"text":"根据"}` | 答案增量文本 |
| `done` | `{"messageId":9,"answer":"完整答案","durationMs":420,"stepCount":1}` | 结束 |
| `error` | `{"message":"..."}` | 异常 |

前端使用 `fetch` + `ReadableStream` 解析（`EventSource` 不支持 POST 与自定义 Header）。
SSE 帧格式：`event: token\ndata: {...}\n\n`。
