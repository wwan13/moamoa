import { http } from "./client"

export type AdminTechBlogSummary = {
  id: number
  title: string
  icon: string
  blogUrl: string
  key: string
  postCount: number
  subscriptionCount: number
}

export type AdminCollectPostsCommand = {
  techBlogId: number
}

export type AdminCollectPostsResult = {
  techBlog: AdminTechBlogSummary
  newPostCount: number
  updatedPostCount: number
}

export type AdminDeleteTechBlogPostsResult = {
  techBlog: AdminTechBlogSummary
  deletedPostCount: number
}

const COLLECT_POSTS_TIMEOUT_MS = 1000 * 60 * 10

export const techBlogApi = {
  findAll: async (): Promise<AdminTechBlogSummary[]> => {
    const res = await http.get<AdminTechBlogSummary[]>("/api/admin/tech-blog")
    if (res) return res

    return []
  },
  collectPosts: async (
    command: AdminCollectPostsCommand,
  ): Promise<AdminCollectPostsResult> => {
    const controller = new AbortController()
    const timeoutId = window.setTimeout(() => {
      controller.abort()
    }, COLLECT_POSTS_TIMEOUT_MS)

    try {
      const res = await http.post<AdminCollectPostsResult>(
        "/api/admin/tech-blog/collect-posts",
        command,
        { signal: controller.signal },
      )

      if (res) return res

      throw new Error("EMPTY_COLLECT_POSTS_RESPONSE")
    } finally {
      window.clearTimeout(timeoutId)
    }
  },
  deletePosts: async (
    techBlogId: number,
  ): Promise<AdminDeleteTechBlogPostsResult> => {
    const res = await http.del<AdminDeleteTechBlogPostsResult>(
      `/api/admin/tech-blog/${techBlogId}/posts`,
    )

    if (res) return res

    throw new Error("EMPTY_DELETE_TECH_BLOG_POSTS_RESPONSE")
  },
}
