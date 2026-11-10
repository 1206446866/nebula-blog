import request from '@/utils/request'
export const exportArticleApi = (id: number, images = false) =>
  request.get<Blob>(`/articles/${id}/export`, {
    params: { images },
    responseType: 'blob',
    timeout: 60000,
  })
export const archiveWritingApi = (title: string, content: string) =>
  request.post<Blob>(
    '/article-export',
    { title, content },
    { responseType: 'blob', timeout: 60000 },
  )
