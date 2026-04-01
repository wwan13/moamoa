import { http, type ApiRequestConfig } from "./client"

export type NoticeSummary = {
  id: number
  title: string
  chip: string
  content: string
  publishedAt: string
}

export type NoticeListMeta = {
  page: number
  size: number
  totalCount: number
  totalPages: number
}

export type NoticeListConditions = {
  page?: number
  size?: number
  query?: string
}

export type NoticeList = {
  meta: NoticeListMeta
  notices: NoticeSummary[]
}

export type NoticeDetail = {
  id: number
  title: string
  chip: string
  content: string
  publishedAt: string
}

const buildQuery = (paramsObj: Record<string, unknown> = {}): string => {
  const params = new URLSearchParams()
  Object.entries(paramsObj).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return
    params.set(key, String(value))
  })

  const q = params.toString()
  return q ? `?${q}` : ""
}

export const noticeApi = {
  list: async (
    conditions: NoticeListConditions = {},
    config?: ApiRequestConfig,
  ): Promise<NoticeList> => {
    const page = conditions.page ?? 1
    const size = conditions.size ?? 10
    const q = buildQuery({
      page: page > 1 ? page : undefined,
      size,
      query: conditions.query || undefined,
    })

    const res = await http.get<NoticeList>(`/api/notice${q}`, config)
    return (
      res ?? {
        meta: { page, size, totalCount: 0, totalPages: 0 },
        notices: [],
      }
    )
  },
  detail: async (
    noticeId: number,
    config?: ApiRequestConfig,
  ): Promise<NoticeDetail | null> => {
    if (!Number.isFinite(noticeId) || noticeId <= 0) return null
    return await http.get<NoticeDetail>(`/api/notice/${noticeId}`, config)
  },
}
