# 简历与面试指南 · AgentDesk

> 这份文档解决三件事：**简历上怎么写**、**面试时怎么讲**、**被追问时怎么答**。
> 所有数字都标注了获取方式，请用你自己机器上的实测值替换，不要直接照抄。

---

## 一、项目一句话

**AgentDesk · 企业知识库智能体工作台**：基于 Java 17 + Spring Boot 3 + Vue 3 的前后端分离项目，自研 ReAct Agent 执行器，实现文档解析、切片向量化、混合检索、工具调用与 SSE 流式回答，并完整记录可回溯的推理轨迹。

技术关键词：`Java 17` `Spring Boot 3` `Spring Security` `JWT` `MyBatis-Plus` `MySQL 8` `ReAct Agent` `RAG` `SSE` `Vue 3` `TypeScript` `Vite` `Pinia` `Element Plus`

---

## 二、简历条目（可直接改）

### 版本 A：一页简历 · 4 条（推荐）

> **AgentDesk · 企业知识库智能体工作台**（个人项目 / 2026.11）
> `Java 17` `Spring Boot 3` `MyBatis-Plus` `MySQL` `Spring Security + JWT` `Vue 3 + TS` `SSE`

- 独立完成前后端分离的企业知识库系统：Spring Boot 3 + MyBatis-Plus + MySQL 提供 JWT 鉴权、文档管理、会话与消息持久化，Vue 3 + TypeScript + Element Plus 实现聊天工作台与知识库管理，**39 个单元测试全部通过**。
- **自研轻量 ReAct Agent 执行器**：抽象 `Tool` 接口与注册表，内置知识库检索、计算器、时间、统计 4 个工具；实现「Prompt 构造 → Action 解析 → 工具调用 → Observation 回填 → Final Answer」推理循环，支持最多 5 轮推理、工具异常降级与重复调用熔断。
- 设计 **RAG 检索链路**：PDFBox 解析 txt/md/pdf，按句子边界切片（320 字 / 重叠 60），384 维哈希向量落库；检索采用**向量余弦 + 关键词 IDF 覆盖率**加权排序，配合 Caffeine 缓存，回答附带来源与相关度。
- 基于 **SSE** 实现流式对话（`meta / step / token / done / error` 五类事件），推理线程池与请求线程隔离；前端实时渲染 Agent 思考轨迹与增量答案，支持中断生成，本地规则模型下首个 token 约 **30ms**、单次回答约 **0.3~0.9s**。

### 版本 B：投 Agent 开发岗（突出 Agent / RAG）

- 从 0 实现 ReAct Agent：工具注册与路由、Thought/Action 解析、Observation 回填、最大轮数与重复调用熔断，单次对话可观测每一步耗时与输入输出。
- 设计混合检索策略（向量 + IDF 关键词加权），为「短查询、专有名词」场景优化召回；切片按句子边界 + 重叠窗口，避免语义截断。
- 实现 Agent 可观测性：推理轨迹序列化入 MySQL（`trace_json`），前端时间线还原历史推理过程，支持问题定位与效果评估。
- 抽象 `ChatModel` 接口同时支持本地规则模型与 OpenAI 兼容接口（DeepSeek / 通义 / OpenAI），无 Key 也可完整演示，方便 CI 与离线环境。

### 版本 C：投 Java 全栈岗（突出工程能力）

- 基于 Spring Boot 3 搭建分层后端：Controller / Service / Mapper 三层，统一响应体与全局异常处理，Bean Validation 参数校验，Swagger 在线文档。
- 使用 Spring Security + JWT 实现无状态鉴权，BCrypt 存储密码，自定义 `OncePerRequestFilter` 解析 Token，统一 401/403 JSON 响应，所有业务查询按 `user_id` 做数据隔离。
- 使用 MyBatis-Plus 完成 CRUD 与手写聚合 SQL（`CHAR_LENGTH` 统计字数），事务保证文档与切片级联删除的一致性。
- 前端使用 Vue 3 + TypeScript + Vite + Pinia + Element Plus，封装 axios 拦截器统一处理 Token 与错误，基于 `fetch` + `ReadableStream` 手写 SSE 解析器（兼容跨 chunk 半包、CRLF），流式渲染时只更新最后一条消息避免列表重渲染。

> 建议：投不同岗位时**调整条目顺序**，把最匹配的 2 条放前面；项目名后面直接跟技术栈，HR 和技术面一眼能看到关键词。

---

## 三、量化数据怎么来（别编）

| 指标 | 获取方式 | 参考值（本机） |
| --- | --- | --- |
| 单元测试数量 | `mvnw.cmd test` 输出 | 39 个，0 失败 |
| 接口数量 | Swagger / `docs/api.md` | 13 个 REST + 1 个 SSE |
| 内置工具数 | `ToolRegistry` 启动日志或代码 | 4 个 |
| 首字延迟 | 浏览器 Network → SSE 首帧时间 | 约 30ms（mock） |
| 单次对话耗时 | `done` 事件 `durationMs` | 约 0.3~0.9s（含逐字输出） |
| 文档入库耗时 | 应用日志 `文档入库完成` | 3 份文档共 13 切片，约 60ms |
| 检索 Top-4 命中率 | 自建 20 题评测集人工标注 | 建议实测后写，如 85% |

**评测集怎么建（加分项）**：准备 20 个问题，标注标准答案所在文档与片段，写一个 JUnit 测试调用 `RetrievalService.search`，统计 Top-4 命中率与 MRR。这个测试本身就是很好的面试素材。

---

## 四、2 分钟讲解稿

> 这个项目叫 AgentDesk，是一个企业知识库智能体。背景是公司制度文档很多，新人找一个报销规则要翻很久，所以我做了一个“上传文档就能问答”的系统。
>
> 整体是前后端分离：后端 Java 17 + Spring Boot 3，MySQL 存文档、切片、会话和消息；前端 Vue 3 + TypeScript。鉴权用 Spring Security + JWT，接口统一响应体和异常处理。
>
> 核心有两块。第一块是 RAG：上传的文档先解析，按句子切成 320 字左右的片段，带 60 字重叠，然后用 384 维哈希向量存到 MySQL；检索时把问题向量和片段向量做余弦相似度，再叠加关键词的 IDF 覆盖率，按 0.65 比 0.35 加权排序，取 Top-4 注入 Prompt。
>
> 第二块是 Agent：我没有直接用 LangChain4j，而是自己实现了 ReAct 执行器。工具实现 Tool 接口注册成 Bean，执行器负责拼 Prompt、解析 Action、调工具、把 Observation 回填，最多 5 轮。这里做了两个保护：连续两轮相同工具和入参就提前结束，工具抛异常也会转成 Observation，不会让整个对话 500。
>
> 交互上用了 SSE，把 meta、step、token、done 分开推，前端右侧会实时显示 Agent 的思考轨迹，刷新页面后历史轨迹还能从数据库还原。这样答案不仅可解释，也方便排查“为什么答错了”。
>
> 项目有 39 个单元测试，覆盖 Agent 循环、切片、向量、检索排序和 JWT。后面我准备把文档解析改成异步，再接入 pgvector 支持更大数据量。

---

## 五、高频追问（20 题）

### Agent / RAG

**1. 为什么自研 Agent，不用 LangChain4j 或 Spring AI？**
框架抽象程度高，出问题不好定位；这个项目想展示我对 Agent 运行机制的理解。自研后工具注册、Prompt 模板、解析规则、熔断策略都可控，也方便写单元测试。真实业务里如果需求标准，我会优先用成熟框架，自研部分只保留业务工具。

**2. ReAct 循环怎么防止死循环？**
三层保护：一是 `agentdesk.agent.max-steps` 限制最多 5 轮；二是检测连续两轮 `tool + input` 相同就 break；三是模型没按格式输出 Action 时，直接把输出当最终答案返回。最后还有兜底文案，把最后一次 Observation 返回给用户。

**3. 工具调用失败怎么办？**
`ToolRegistry.execute` 抛出的异常会被 `AgentExecutor` 捕获，转成 `"工具执行失败：xxx"` 的 Observation 回填给模型，模型可以决定换工具或直接说明。这样单次工具故障不会导致整个请求 500。

**4. 多轮对话上下文怎么控制？**
只取最近 6 条消息（可配置）拼进 Prompt，并且只保留 user/assistant 的 content，不带历史 trace。更长的对话可以做摘要压缩，这是 Roadmap 里的下一项。

**5. 为什么不用向量数据库？**
当前规模是演示级（万级切片以内），MySQL 存向量 + 内存余弦足够，少一个中间件就少一个故障点。`EmbeddingModel` 和 `RetrievalService` 已经抽象好，换成 pgvector 或 Milvus 只需要改检索实现，Agent 层不动。

**6. 混合检索的权重怎么定的？**
初始值 0.65 向量 + 0.35 关键词，是在示例文档上人工调出来的：纯向量对“差旅报销”这类专有名词容易漂移，纯关键词又不懂同义表达。关键词分不是简单命中率，而是按 IDF 加权——越少见的词权重越高，更接近 BM25 的思路。

**7. 切片大小和重叠怎么选？**
320 字大约是一个制度条款的完整语义单元，太大容易混入无关内容、太小会截断条件。60 字重叠保证跨切片的句子不被腰斩。切分优先按句号/问号/分号，超长句子再硬切。

**8. 怎么评估 RAG 效果？**
两层：检索层看 Top-K 命中率、MRR；生成层看答案是否引用了正确片段、是否有幻觉。我会建 20 题的小评测集，用 JUnit 跑回归，避免改完切片参数后效果变差还不知道。

### Java / Spring

**9. 为什么用 SSE 不用 WebSocket？**
场景是“客户端问、服务端单向流式返回”，SSE 基于 HTTP，天然支持断线重连、无需额外协议升级，网关和鉴权都更简单。WebSocket 更适合双向实时协作。SSE 用 POST 时不能用 `EventSource`，所以前端用 `fetch` + `ReadableStream` 自己解析事件帧。

**10. 流式接口怎么避免占用 Tomcat 线程？**
Controller 只负责创建 `SseEmitter` 并返回，真正的 Agent 推理丢到独立的 `ExecutorService`（8 个线程）里执行，请求线程立刻释放。emitter 设置 180 秒超时，客户端断开时捕获异常并结束。

**11. JWT 怎么退出登录？**
JWT 本身无状态，退出就是前端删除 Token。要做到服务端失效需要引入黑名单（Redis）或短 Token + Refresh Token，这个项目为了简洁没做，面试时我会说明这是有意的取舍。

**12. 怎么保证用户 A 看不到用户 B 的数据？**
所有查询都带 `user_id` 条件，删除/查询前先 `requireConversation` / 校验 `document.userId`，越权返回 404 而不是 403，避免泄露资源是否存在。Token 里的 userId 由服务端签发，不信任前端传参。

**13. 上传和解析异常怎么处理？**
三层校验：文件非空、大小 ≤10MB、扩展名白名单；解析失败（如加密 PDF）抛业务异常返回 400；文本为空也拒绝入库。切片结果为空同样拦截，保证数据库里不会出现 0 切片的文档。

**14. 事务用在哪？**
文档入库（文档 + N 个切片）和删除（切片 + 文档）都在 `@Transactional` 里，避免中途失败留下脏数据。SSE 对话不在一个事务里，因为流式过程中要多次写库，长事务会占用连接。

**15. 表结构和索引怎么设计的？**
5 张表：`users`、`documents`、`document_chunks`、`conversations`、`messages`。高频查询都走 `user_id` / `conversation_id`，建了对应普通索引；`messages` 按会话查、按 id 排序，`(conversation_id, id)` 的组合索引会更优，数据量大时可以补。

### 前端

**16. 流式渲染怎么保证不卡？**
不要每来一个 token 就重建消息数组。我只更新最后一条 assistant 消息的 `content`，列表 key 用消息 id 保持稳定；滚动用 `requestAnimationFrame` 节流；Markdown 渲染做了 DOMPurify 消毒。

**17. SSE 解析要注意什么？**
TCP 分片不代表事件完整：要维护 buffer，按 `\n\n` 切帧，处理 `\r\n`、多行 `data`、跨 chunk 的半包，以及流结束时残留的最后一帧。这些边界条件我抽成了独立的 `sse.ts` 解码器，方便测试。

### 系统设计

**18. Mock 模型有什么意义？**
保证项目零配置可运行：面试官 clone 下来不配 Key 也能看到完整 Agent 流程；CI 里也不依赖外部网络。它不是写死的假数据，而是按问题类型选择工具、解析 Observation、做句子级抽取摘要，和真实模型的输入输出协议完全一致。

**19. QPS 上来怎么扩展？**
应用本身无状态，可以水平扩容；瓶颈会先出现在两处：文档入库的同步解析（改异步 + 消息队列）和检索时的全量切片加载（换向量库 + 分页/分区）。MySQL 连接池目前 10，按压测结果调整。

**20. 这个项目最大的不足？**
三个：没有真正的多租户与权限体系；检索是内存暴力计算，数据量大了要换向量库；对话记忆没有摘要，长对话会丢上下文。我在 Roadmap 里列了对应的演进方案，也清楚边界在哪。

---

## 六、3 分钟现场演示脚本

1. **登录**：用 `demo / 123456` 登录，指出左侧会话列表、右侧轨迹面板（20s）。
2. **问制度**：输入“差旅报销多久内提交？”，边等边讲 SSE 的 `step` 事件如何驱动轨迹面板刷新（40s）。
3. **看轨迹**：展开 Observation，说明检索到的片段、score 与耗时（30s）。
4. **换工具**：问“128*3+45 等于多少”，展示多工具路由（20s）。
5. **知识库页**：展示统计、上传一份 md、用检索测试抽屉看 Top-K 与 score（50s）。
6. **Swagger**：打开 `/swagger-ui.html`，说明接口设计与统一响应体（20s）。
7. **收尾**：说清技术取舍与下一步规划（20s）。

---

## 七、GitHub 仓库包装建议

- **About**：`Java 17 · Spring Boot 3 · Vue 3 的全栈 RAG Agent 项目：自研 ReAct 执行器、混合检索、SSE 流式对话与推理轨迹可视化`
- **Topics**：`java` `spring-boot` `agent` `react-agent` `rag` `llm` `sse` `vue3` `typescript` `mysql` `mybatis-plus`
- **README**：首屏放截图 + 一句话 + 技术栈徽章，面试官 10 秒内能判断项目含金量；
- **Commit**：按功能拆分，例如 `feat(agent): 实现 ReAct 执行器与工具注册表`、`feat(rag): 混合检索与切片优化`、`test: 补充 Agent 与检索单测`，比一次 `init` 更能体现工程习惯；
- **Issue / Projects**：把 Roadmap 拆成 issue，展示持续迭代意识。

---

## 八、自查清单（避免减分）

- [ ] 能在 2 分钟内讲清楚“请求进来后发生了什么”
- [ ] 能白板画出表关系与 Agent 循环
- [ ] 每个技术选型都能说出“为什么不用另一个”
- [ ] 简历上的数字都能在代码或日志里找到依据
- [ ] 演示环境不依赖外网与 API Key
- [ ] 知道项目的不足，并能给出演进方案
