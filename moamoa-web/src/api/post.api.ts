import { http, type ApiRequestConfig } from "./client"

const DEFAULT_PAGE_SIZE = 20
const CATEGORY_ID_BY_KEY = {
  engineering: 10,
  product: 20,
  design: 30,
  etc: 40,
} as const

export type PostCategoryKey = "all" | keyof typeof CATEGORY_ID_BY_KEY

export type PostListConditions = {
  page?: number
  size?: number
  query?: string
  category?: PostCategoryKey | number | string
}

export type TechBlogPostConditions = {
  page?: number
  techBlogId?: string | number
  category?: PostCategoryKey | number | string
}

export type PostPagingConditions = {
  page?: number
  category?: PostCategoryKey | number | string
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
  categoryId: number
  thumbnail: string
  url: string
  publishedAt: string
  isBookmarked: boolean
  viewCount: number
  bookmarkCount: number
  techBlogId: number
  techBlogTitle: string
  techBlogIcon: string
  techBlogBlogUrl: string
  techBlogKey: string
  techBlogSubscriptionCount: number
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

const resolveCategoryId = (category?: PostCategoryKey | number | string): number | undefined => {
  if (category === undefined || category === null || category === "") return undefined
  if (typeof category === "number") return category > 0 ? category : undefined

  const normalized = String(category).trim().toLowerCase()
  if (!normalized || normalized === "all") return undefined
  const id = CATEGORY_ID_BY_KEY[normalized as keyof typeof CATEGORY_ID_BY_KEY]
  return id
}

export const postsApi = {
  list: async (conditions: PostListConditions = {}, config?: ApiRequestConfig): Promise<PostList> => {
    const page = conditions.page
    const size = conditions.size ?? DEFAULT_PAGE_SIZE
    const q = buildQuery({
      page: page && page > 1 ? page : undefined,
      size,
      query: conditions.query || undefined,
      category: resolveCategoryId(conditions.category),
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
      category: resolveCategoryId(conditions.category),
    })

    const res = await http.get<PostList>(`/api/post/tech-blog${q}`, config)
    return res ?? emptyPostList(page ?? 1, DEFAULT_PAGE_SIZE)
  },
  listBySubscription: async (
    conditions: PostPagingConditions = {},
    config?: ApiRequestConfig
  ): Promise<PostList> => {
    const page = conditions.page
    const q = buildQuery({
      page: page && page > 1 ? page : undefined,
      size: DEFAULT_PAGE_SIZE,
      category: resolveCategoryId(conditions.category),
    })
    const res = await http.get<PostList>(`/api/post/subscribed${q}`, config)
    return res ?? emptyPostList(page ?? 1, DEFAULT_PAGE_SIZE)
  },
  listByBookmark: async (
    conditions: PostPagingConditions = {},
    config?: ApiRequestConfig
  ): Promise<PostList> => {
    const page = conditions.page
    const q = buildQuery({
      page: page && page > 1 ? page : undefined,
      size: DEFAULT_PAGE_SIZE,
      category: resolveCategoryId(conditions.category),
    })
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
