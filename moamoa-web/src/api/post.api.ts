import { http, type ApiRequestConfig } from "./client"
import type { TechBlogData } from "./techBlog.api"

const DEFAULT_PAGE_SIZE = 20

export type PostListConditions = {
  page?: number
  size?: number
  query?: string
}

export type TechBlogPostConditions = {
  page?: number
  techBlogId?: string | number
}

export type PostPagingConditions = {
  page?: number
}

export type IncreasePostViewCountCommand = {
  postId: number | string
}

export type IncreaseViewCountResult = {
  success: boolean
}

export type PostSummary = {
  id: number
  key: string
  title: string
  description: string
  thumbnail: string
  url: string
  publishedAt: string
  isBookmarked: boolean
  viewCount: number
  bookmarkCount: number
  techBlog: TechBlogData
}

export type PostListMeta = {
  page: number
  size: number
  totalCount: number
  totalPages: number
}

export type PostList = {
  meta: PostListMeta
  posts: PostSummary[]
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

const emptyPostList = (page = 1, size = DEFAULT_PAGE_SIZE): PostList => ({
  meta: { page, size, totalCount: 0, totalPages: 0 },
  posts: [],
})

export const postsApi = {
  list: async (conditions: PostListConditions = {}, config?: ApiRequestConfig): Promise<PostList> => {
    const page = conditions.page
    const size = conditions.size ?? DEFAULT_PAGE_SIZE
    const q = buildQuery({
      page: page && page > 1 ? page : undefined,
      size,
      query: conditions.query || undefined,
    })

    const res = await http.get<PostList>(`/api/post${q}`, config)
    return res ?? emptyPostList(page ?? 1, size)
  },
  listByTechBlogId: async (
    conditions: TechBlogPostConditions = {},
    config?: ApiRequestConfig
  ): Promise<PostList> => {
    const page = conditions.page
    const q = buildQuery({
      page: page && page > 1 ? page : undefined,
      size: DEFAULT_PAGE_SIZE,
      techBlogId: conditions.techBlogId,
    })

    const res = await http.get<PostList>(`/api/post/tech-blog${q}`, config)
    return res ?? emptyPostList(page ?? 1, DEFAULT_PAGE_SIZE)
  },
  listBySubscription: async (
    conditions: PostPagingConditions = {},
    config?: ApiRequestConfig
  ): Promise<PostList> => {
    const page = conditions.page
    const q = buildQuery({ page: page && page > 1 ? page : undefined, size: DEFAULT_PAGE_SIZE })
    const res = await http.get<PostList>(`/api/post/subscribed${q}`, config)
    return res ?? emptyPostList(page ?? 1, DEFAULT_PAGE_SIZE)
  },
  listByBookmark: async (
    conditions: PostPagingConditions = {},
    config?: ApiRequestConfig
  ): Promise<PostList> => {
    const page = conditions.page
    const q = buildQuery({ page: page && page > 1 ? page : undefined, size: DEFAULT_PAGE_SIZE })
    const res = await http.get<PostList>(`/api/post/bookmarked${q}`, config)
    return res ?? emptyPostList(page ?? 1, DEFAULT_PAGE_SIZE)
  },
  increaseViewCount: async (
    command: IncreasePostViewCountCommand,
    config?: ApiRequestConfig
  ): Promise<IncreaseViewCountResult> => {
    const res = await http.post<IncreaseViewCountResult>(`/api/post/${command.postId}/view`, {}, config)
    return res ?? { success: false }
  },
}
