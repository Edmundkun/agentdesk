import { request } from './http'
import type { KnowledgeDocument, KnowledgeStats, SearchHit } from './types'

/** GET /knowledge/stats */
export function getStats(): Promise<KnowledgeStats> {
  return request<KnowledgeStats>({ url: '/knowledge/stats', method: 'get' })
}

/** GET /knowledge/documents */
export function getDocuments(): Promise<KnowledgeDocument[]> {
  return request<KnowledgeDocument[]>({ url: '/knowledge/documents', method: 'get' })
}

/** POST /knowledge/documents （multipart/form-data，字段名 file） */
export function uploadDocument(
  file: File,
  onProgress?: (percent: number) => void,
): Promise<KnowledgeDocument> {
  const formData = new FormData()
  formData.append('file', file)
  return request<KnowledgeDocument>({
    url: '/knowledge/documents',
    method: 'post',
    data: formData,
    timeout: 120_000,
    onUploadProgress: (event) => {
      if (!onProgress) return
      if (event.total) {
        onProgress(Math.min(100, Math.round((event.loaded / event.total) * 100)))
      }
    },
  })
}

/** DELETE /knowledge/documents/{id} */
export function deleteDocument(id: number): Promise<null> {
  return request<null>({ url: `/knowledge/documents/${id}`, method: 'delete' })
}

/** POST /knowledge/search */
export function searchKnowledge(query: string, topK = 4): Promise<SearchHit[]> {
  return request<SearchHit[]>({
    url: '/knowledge/search',
    method: 'post',
    data: { query, topK },
  })
}
