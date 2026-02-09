import { http, type ApiRequestConfig } from "./client"

export type TechBlogData = {
  id: number
  title: string
  icon: string
  blogUrl: string
  key: string
  subscriptionCount: number
}

export type TechBlogSummary = TechBlogData & {
  postCount: number
  subscribed: boolean
  notificationEnabled: boolean
}

export type TechBlogList = {
  meta: {
    totalCount: number
  }
  techBlogs: TechBlogSummary[]
}

export type TechBlogListConditions = {
  query?: string
}

export type TechBlogIdCondition = {
  techBlogId: string | number
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

export const techBlogApi = {
  list: async (conditions: TechBlogListConditions = {}, config?: ApiRequestConfig): Promise<TechBlogList> => {
    const q = buildQuery({ query: conditions.query || undefined })
    const res = await http.get<TechBlogList>(`/api/tech-blog${q}`, config)
    return res ?? { meta: { totalCount: 0 }, techBlogs: [] }
  },
  findById: async (condition: TechBlogIdCondition, config?: ApiRequestConfig): Promise<TechBlogSummary | null> => {
    return await http.get<TechBlogSummary>(`/api/tech-blog/${condition.techBlogId}`, config)
  },
  listSubscribed: async (config?: ApiRequestConfig): Promise<TechBlogList> => {
    const res = await http.get<TechBlogList>("/api/tech-blog/subscription", config)
    return res ?? { meta: { totalCount: 0 }, techBlogs: [] }
  },
}
