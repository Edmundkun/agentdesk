<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRawFile, UploadRequestOptions } from 'element-plus'
import {
  Delete,
  Document,
  EditPen,
  Files,
  Refresh,
  Search,
  UploadFilled,
} from '@element-plus/icons-vue'
import type { Component } from 'vue'
import AppShell from '../components/AppShell.vue'
import { deleteDocument, getDocuments, getStats, searchKnowledge, uploadDocument } from '../api/knowledge'
import type { KnowledgeDocument, KnowledgeStats, SearchHit } from '../api/types'
import { formatBytes, formatDateTime, formatNumber } from '../utils/format'

interface StatCard {
  label: string
  value: string
  icon: Component
  tone: string
}

const stats = ref<KnowledgeStats>({ documentCount: 0, chunkCount: 0, totalChars: 0 })
const documents = ref<KnowledgeDocument[]>([])
const loadingDocuments = ref(false)

const uploading = ref(false)
const uploadPercent = ref(0)
const uploadName = ref('')

const drawerVisible = ref(false)
const searchQuery = ref('')
const searching = ref(false)
const searched = ref(false)
const results = ref<SearchHit[]>([])

const statCards = computed<StatCard[]>(() => [
  { label: '文档数', value: formatNumber(stats.value.documentCount), icon: Document, tone: 'indigo' },
  { label: '知识片段', value: formatNumber(stats.value.chunkCount), icon: Files, tone: 'cyan' },
  { label: '总字数', value: formatNumber(stats.value.totalChars), icon: EditPen, tone: 'amber' },
])

const fileTypeMap: Record<string, 'primary' | 'success' | 'info' | 'warning' | 'danger'> = {
  pdf: 'danger',
  md: 'primary',
  txt: 'info',
}

function tagType(fileType: string): 'primary' | 'success' | 'info' | 'warning' | 'danger' {
  return fileTypeMap[fileType.toLowerCase()] ?? 'info'
}

function statusText(status: string): string {
  const map: Record<string, string> = { READY: '就绪', PROCESSING: '处理中', FAILED: '处理失败' }
  return map[status] ?? status
}

function statusClass(status: string): string {
  if (status === 'READY') return 'is-ready'
  return status === 'FAILED' ? 'is-failed' : 'is-pending'
}

function scorePercent(score: number): number {
  return Math.min(100, Math.max(0, Math.round(score * 100)))
}

async function loadData(): Promise<void> {
  loadingDocuments.value = true
  try {
    const [statsResult, documentsResult] = await Promise.all([getStats(), getDocuments()])
    stats.value = statsResult
    documents.value = documentsResult
  } catch {
    /* 拦截器已统一提示 */
  } finally {
    loadingDocuments.value = false
  }
}

function beforeUpload(file: UploadRawFile): boolean {
  const extension = file.name.split('.').pop()?.toLowerCase() ?? ''
  if (!['txt', 'md', 'pdf'].includes(extension)) {
    ElMessage.error('仅支持 TXT / Markdown / PDF 文件')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('单个文件大小不能超过 10MB')
    return false
  }
  return true
}

async function handleUpload(options: UploadRequestOptions): Promise<void> {
  const file = options.file
  uploading.value = true
  uploadPercent.value = 0
  uploadName.value = file.name
  try {
    const document = await uploadDocument(file, (percent) => {
      uploadPercent.value = percent
    })
    options.onSuccess(document)
    ElMessage.success(`《${file.name}》上传成功，已完成解析与切片`)
    await loadData()
  } catch {
    /* 拦截器已统一提示 */
  } finally {
    uploading.value = false
    uploadPercent.value = 0
    uploadName.value = ''
  }
}

/** el-table 插槽的 row 类型为 DefaultRow，这里做一次显式收敛 */
function handleDeleteRow(row: Record<string, unknown>): void {
  void handleDelete(row as unknown as KnowledgeDocument)
}

async function handleDelete(document: KnowledgeDocument): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `删除《${document.name}》后，其 ${document.chunkCount} 个知识片段也将一并移除，确定继续吗？`,
      '删除文档',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
      },
    )
  } catch {
    return
  }
  try {
    await deleteDocument(document.id)
    ElMessage.success('文档已删除')
    await loadData()
  } catch {
    /* 拦截器已统一提示 */
  }
}

async function runSearch(): Promise<void> {
  const query = searchQuery.value.trim()
  if (!query) {
    ElMessage.warning('请输入要检索的内容')
    return
  }
  searching.value = true
  try {
    results.value = await searchKnowledge(query, 4)
    searched.value = true
  } catch {
    results.value = []
    searched.value = true
  } finally {
    searching.value = false
  }
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <AppShell>
    <div class="knowledge-page">
      <div class="page-inner">
        <header class="page-head">
          <div>
            <h1>知识库</h1>
            <p>上传企业文档，Agent 回答时会先检索这些知识片段，再组织答案与推理轨迹。</p>
          </div>
          <el-button type="primary" @click="drawerVisible = true">
            <el-icon :size="14" class="btn-icon"><Search /></el-icon>
            检索测试
          </el-button>
        </header>

        <section class="stat-grid">
          <article v-for="card in statCards" :key="card.label" class="stat-card surface-card">
            <span class="stat-icon" :class="card.tone">
              <el-icon :size="18"><component :is="card.icon" /></el-icon>
            </span>
            <div class="stat-body">
              <p class="stat-label">{{ card.label }}</p>
              <p class="stat-value">{{ card.value }}</p>
            </div>
          </article>
        </section>

        <section class="knowledge-top">
          <article class="upload-card surface-card">
            <div class="card-head">
              <div>
                <h2 class="card-title">上传文档</h2>
                <p class="card-desc">文档会被自动解析、切分为可检索的知识片段</p>
              </div>
              <span class="card-chip">TXT / MD / PDF</span>
            </div>

            <el-upload
              class="knowledge-uploader"
              drag
              accept=".txt,.md,.pdf"
              :show-file-list="false"
              :disabled="uploading"
              :http-request="handleUpload"
              :before-upload="beforeUpload"
            >
              <div class="upload-inner">
                <span class="upload-icon">
                  <el-icon :size="22"><UploadFilled /></el-icon>
                </span>
                <p class="upload-title">拖拽文件到此处，或 <em>点击选择文件</em></p>
                <p class="upload-hint">单个文件不超过 10MB，支持 .txt / .md / .pdf</p>
              </div>
            </el-upload>

            <div v-if="uploading" class="upload-progress">
              <div class="progress-head">
                <span class="progress-name">{{ uploadName }}</span>
                <span class="progress-value">{{ uploadPercent }}%</span>
              </div>
              <el-progress
                :percentage="uploadPercent"
                :stroke-width="6"
                :show-text="false"
                :status="uploadPercent >= 100 ? 'success' : undefined"
              />
              <p class="progress-tip">正在上传并解析文档，请稍候…</p>
            </div>
          </article>

          <aside class="tips-card surface-card">
            <h2 class="card-title">切片流程</h2>
            <ol class="tips-list">
              <li>
                <span class="tip-index">1</span>
                <div>
                  <p class="tip-title">上传解析</p>
                  <p class="tip-desc">读取文本内容，保留标题层级</p>
                </div>
              </li>
              <li>
                <span class="tip-index">2</span>
                <div>
                  <p class="tip-title">智能切片</p>
                  <p class="tip-desc">按语义切分为带序号的片段</p>
                </div>
              </li>
              <li>
                <span class="tip-index">3</span>
                <div>
                  <p class="tip-title">检索增强</p>
                  <p class="tip-desc">提问时召回 Top-K 片段参与推理</p>
                </div>
              </li>
            </ol>
            <p class="tips-note">可用右上角「检索测试」直接验证切片与召回效果。</p>
          </aside>
        </section>

        <section class="table-card surface-card">
          <div class="card-head table-head">
            <div>
              <h2 class="card-title">文档列表</h2>
              <p class="card-desc">共 {{ documents.length }} 份文档，删除后其片段将同步移除</p>
            </div>
            <el-button text size="small" @click="loadData">
              <el-icon :size="13"><Refresh /></el-icon>
              刷新
            </el-button>
          </div>

          <el-table
            v-loading="loadingDocuments"
            :data="documents"
            row-key="id"
            empty-text="还没有文档，先上传一份试试"
          >
            <el-table-column label="文档名称" min-width="280">
              <template #default="{ row }">
                <div class="doc-cell">
                  <span class="doc-icon">
                    <el-icon :size="14"><Document /></el-icon>
                  </span>
                  <div class="doc-meta">
                    <span class="doc-title">{{ row.name }}</span>
                    <span class="doc-status" :class="statusClass(row.status)">
                      {{ statusText(row.status) }}
                    </span>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="类型" width="90">
              <template #default="{ row }">
                <el-tag :type="tagType(row.fileType)" size="small" effect="light">
                  {{ String(row.fileType).toUpperCase() }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="片段数" width="96" prop="chunkCount" />
            <el-table-column label="大小" width="110">
              <template #default="{ row }">{{ formatBytes(row.sizeBytes) }}</template>
            </el-table-column>
            <el-table-column label="创建时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="86" align="right">
              <template #default="{ row }">
                <el-button link type="danger" :icon="Delete" @click="handleDeleteRow(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </div>
    </div>

    <el-drawer v-model="drawerVisible" title="检索测试" size="480px" :destroy-on-close="false">
      <div class="drawer-body">
        <p class="drawer-tip">
          直接调用 <code>/knowledge/search</code>，验证文档切片与向量召回效果（不经过 Agent）。
        </p>

        <div class="search-bar">
          <el-input
            v-model="searchQuery"
            placeholder="输入问题，例如：差旅报销多久内提交？"
            clearable
            @keyup.enter="runSearch"
          />
          <el-button type="primary" :loading="searching" @click="runSearch">检索</el-button>
        </div>

        <div v-if="searching" class="search-loading">
          <el-skeleton :rows="3" animated />
        </div>

        <template v-else>
          <div v-if="searched && results.length === 0" class="search-empty">
            <el-icon :size="20"><Search /></el-icon>
            <p class="empty-title">没有命中任何片段</p>
            <p class="empty-desc">换个说法，或先上传相关文档再试一次。</p>
          </div>

          <div v-else-if="results.length" class="hit-list">
            <div class="hit-summary">命中 {{ results.length }} 个片段，按相似度排序</div>
            <article
              v-for="(hit, index) in results"
              :key="`${hit.documentId}-${hit.chunkIndex}-${index}`"
              class="hit-card"
            >
              <div class="hit-head">
                <span class="hit-doc">
                  <el-icon :size="13"><Document /></el-icon>
                  {{ hit.documentName }}
                </span>
                <span class="hit-chunk">片段 #{{ hit.chunkIndex }}</span>
              </div>
              <p class="hit-content">{{ hit.content }}</p>
              <div class="hit-score">
                <el-progress
                  class="score-bar"
                  :percentage="scorePercent(hit.score)"
                  :stroke-width="4"
                  :show-text="false"
                />
                <span class="score-text">{{ (hit.score * 100).toFixed(1) }}%</span>
              </div>
            </article>
          </div>

          <div v-else class="search-hint">
            <p>输入一个与文档相关的问题，看看能召回哪些片段。</p>
          </div>
        </template>
      </div>
    </el-drawer>
  </AppShell>
</template>

<style scoped>
.knowledge-page {
  flex: 1;
  min-height: 0;
  padding: 28px 32px 40px;
  overflow-y: auto;
}

.page-inner {
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 22px;
}

.page-head h1 {
  color: #171b2c;
  font-size: 21px;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.page-head p {
  margin-top: 5px;
  color: #8b93a7;
  font-size: 12.5px;
}

.btn-icon {
  margin-right: 4px;
}

/* ------------------------------- 统计卡片 ------------------------------- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
}

.stat-icon {
  display: flex;
  flex: 0 0 42px;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 11px;
}

.stat-icon.indigo {
  background: #eef2ff;
  color: #4f46e5;
}

.stat-icon.cyan {
  background: #ecfeff;
  color: #0891b2;
}

.stat-icon.amber {
  background: #fffbeb;
  color: #d97706;
}

.stat-body {
  min-width: 0;
  line-height: 1.3;
}

.stat-label {
  color: #8b93a7;
  font-size: 11.5px;
}

.stat-value {
  margin-top: 3px;
  color: #171b2c;
  font-size: 22px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.3px;
}

/* ------------------------------- 上传 & 说明 ------------------------------ */
.knowledge-top {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
  margin-bottom: 16px;
}

.upload-card,
.tips-card,
.table-card {
  padding: 20px 22px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.card-title {
  color: #1e2235;
  font-size: 13.5px;
  font-weight: 600;
}

.card-desc {
  margin-top: 3px;
  color: #9299ad;
  font-size: 11.5px;
}

.card-chip {
  flex: 0 0 auto;
  padding: 3px 8px;
  border: 1px solid #eceef6;
  border-radius: 20px;
  background: #fafbfe;
  color: #9299ad;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.3px;
}

.knowledge-uploader :deep(.el-upload) {
  width: 100%;
}

.knowledge-uploader :deep(.el-upload-dragger) {
  width: 100%;
  padding: 26px 20px;
  border: 1px dashed #d8dbea;
  border-radius: 12px;
  background: #fbfbfe;
  transition: border-color 0.15s ease, background-color 0.15s ease;
}

.knowledge-uploader :deep(.el-upload-dragger:hover) {
  border-color: #b7b1f0;
  background: #f8f7ff;
}

.upload-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.upload-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  margin-bottom: 6px;
  border: 1px solid #e3e0fb;
  border-radius: 11px;
  background: #f4f3ff;
  color: #5b55c4;
}

.upload-title {
  color: #3f4557;
  font-size: 13px;
}

.upload-title em {
  color: var(--color-primary);
  font-style: normal;
  font-weight: 500;
}

.upload-hint {
  color: #a2a8ba;
  font-size: 11px;
}

.upload-progress {
  margin-top: 14px;
  padding: 12px 14px;
  border: 1px solid #eceef6;
  border-radius: 10px;
  background: #fbfbfe;
}

.progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 7px;
  font-size: 11.5px;
}

.progress-name {
  overflow: hidden;
  color: #4b5063;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.progress-value {
  flex: 0 0 auto;
  color: #5b55c4;
  font-variant-numeric: tabular-nums;
}

.progress-tip {
  margin-top: 7px;
  color: #a2a8ba;
  font-size: 10.5px;
}

.tips-list {
  display: flex;
  flex-direction: column;
  gap: 13px;
  margin-top: 16px;
}

.tips-list li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.tip-index {
  display: flex;
  flex: 0 0 20px;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  margin-top: 1px;
  border-radius: 6px;
  background: #f1effd;
  color: #5b55c4;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
}

.tip-title {
  color: #3f4557;
  font-size: 12px;
  font-weight: 500;
}

.tip-desc {
  margin-top: 1px;
  color: #9299ad;
  font-size: 11px;
  line-height: 1.6;
}

.tips-note {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed #eceef6;
  color: #a2a8ba;
  font-size: 10.5px;
  line-height: 1.7;
}

/* -------------------------------- 表格 -------------------------------- */
.table-head {
  margin-bottom: 6px;
}

.table-card :deep(.el-table) {
  background: transparent;
}

.table-card :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.doc-cell {
  display: flex;
  align-items: center;
  gap: 9px;
}

.doc-icon {
  display: flex;
  flex: 0 0 28px;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid #eceef6;
  border-radius: 8px;
  background: #fafbfe;
  color: #7c849a;
}

.doc-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.4;
}

.doc-title {
  overflow: hidden;
  color: #262b3d;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-status {
  margin-top: 1px;
  font-size: 10px;
}

.doc-status.is-ready {
  color: #16a34a;
}

.doc-status.is-pending {
  color: #d97706;
}

.doc-status.is-failed {
  color: #dc2626;
}

/* -------------------------------- Drawer ------------------------------- */
.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.drawer-tip {
  color: #8b93a7;
  font-size: 11.5px;
  line-height: 1.7;
}

.drawer-tip code {
  padding: 1px 5px;
  border-radius: 4px;
  background: #f1f3f9;
  color: #4338ca;
  font-family: var(--font-mono);
  font-size: 10.5px;
}

.search-bar {
  display: flex;
  gap: 8px;
}

.search-bar :deep(.el-input__wrapper) {
  border-radius: 9px;
}

.search-loading {
  padding-top: 6px;
}

.search-empty,
.search-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 16px;
  color: #a2a8ba;
  text-align: center;
}

.search-hint p {
  font-size: 11.5px;
}

.search-empty .empty-title {
  margin-top: 10px;
  color: #4b5063;
  font-size: 13px;
  font-weight: 500;
}

.search-empty .empty-desc {
  margin-top: 4px;
  font-size: 11.5px;
}

.hit-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hit-summary {
  color: #a2a8ba;
  font-size: 10.5px;
}

.hit-card {
  padding: 12px 13px;
  border: 1px solid var(--color-border);
  border-radius: 11px;
  background: #fff;
}

.hit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 7px;
}

.hit-doc {
  display: flex;
  align-items: center;
  gap: 5px;
  overflow: hidden;
  color: #4338ca;
  font-size: 11.5px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hit-chunk {
  flex: 0 0 auto;
  padding: 1px 6px;
  border-radius: 5px;
  background: #f5f5fb;
  color: #9299ad;
  font-family: var(--font-mono);
  font-size: 10px;
}

.hit-content {
  display: -webkit-box;
  overflow: hidden;
  color: #4b5063;
  font-size: 11.8px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
}

.hit-score {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 10px;
}

.score-bar {
  flex: 1;
}

.score-text {
  flex: 0 0 auto;
  color: #5b55c4;
  font-family: var(--font-mono);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 1080px) {
  .knowledge-page {
    padding: 22px 20px 32px;
  }

  .knowledge-top {
    grid-template-columns: 1fr;
  }

  .stat-grid {
    gap: 10px;
  }
}

@media (max-width: 640px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }

  .page-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
