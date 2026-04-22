import { http } from "./client"

export type AdminNoticeQueryConditions = {
  page?: number
  size?: number
  query?: string
  published?: boolean
}

export type AdminNoticeSummary = {
  id: number
  title: string
  chip: string
  content: string
  published: boolean
  publishedAt: string
}

type AdminNoticeListMeta = {
  page: number
  size: number
  totalCount: number
  totalPages: number
}

export type AdminNoticeList = {
  meta: AdminNoticeListMeta
  notices: AdminNoticeSummary[]
}

export type AdminUpdateNoticePublishedCommand = {
  published: boolean
}

const buildNoticeQueryString = (
  conditions: AdminNoticeQueryConditions,
): string => {
  const params = new URLSearchParams()

  if (conditions.page !== undefined) params.set("page", String(conditions.page))
  if (conditions.size !== undefined) params.set("size", String(conditions.size))
  if (conditions.query) params.set("query", conditions.query)
  if (conditions.published !== undefined) {
    params.set("published", String(conditions.published))
  }

  const queryString = params.toString()
  return queryString ? `?${queryString}` : ""
}

export const noticeApi = {
  findByConditions: async (
    conditions: AdminNoticeQueryConditions,
  ): Promise<AdminNoticeList> => {
    const queryString = buildNoticeQueryString(conditions)
    const res = await http.get<AdminNoticeList>(`/api/admin/notice${queryString}`)
    if (res) return res

    return {
      meta: {
        page: conditions.page ?? 1,
        size: conditions.size ?? 20,
        totalCount: 0,
        totalPages: 0,
      },
      notices: [],
    }
  },
  updatePublished: async (
    noticeId: number,
    command: AdminUpdateNoticePublishedCommand,
  ): Promise<void> => {
    await http.post<void>(`/api/admin/notice/published/${noticeId}`, command)
  },
}
