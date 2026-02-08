import { http } from "./client"

export type AdminPostQueryConditions = {
    page?: number
    size?: number
    query?: string
    categoryId?: number
    techBlogIds?: number[]
}

type AdminTag = {
    id: number
    title: string
}

type AdminTechBlogData = {
    id: number
    title: string
    icon: string
    blogUrl: string
    key: string
}

export type AdminPostSummary = {
    postId: number
    key: string
    title: string
    description: string
    thumbnail: string
    url: string
    publishedAt: string
    categoryId: number
    techBlog: AdminTechBlogData
    tags: AdminTag[]
}

type AdminPostListMeta = {
    page: number
    size: number
    totalCount: number
    totalPages: number
}

export type AdminPostList = {
    meta: AdminPostListMeta
    posts: AdminPostSummary[]
}

function buildPostQueryString(conditions: AdminPostQueryConditions): string {
    const params = new URLSearchParams()

    if (conditions.page !== undefined) params.set("page", String(conditions.page))
    if (conditions.size !== undefined) params.set("size", String(conditions.size))
    if (conditions.query) params.set("query", conditions.query)
    if (conditions.categoryId !== undefined) {
        params.set("categoryId", String(conditions.categoryId))
    }
    if (conditions.techBlogIds && conditions.techBlogIds.length > 0) {
        conditions.techBlogIds.forEach((id) =>
            params.append("techBlogIds", String(id))
        )
    }

    const queryString = params.toString()
    return queryString ? `?${queryString}` : ""
}

export const postApi = {
    findByConditions: async (
        conditions: AdminPostQueryConditions
    ): Promise<AdminPostList> => {
        const queryString = buildPostQueryString(conditions)
        const res = await http.get<AdminPostList>(`/api/admin/post${queryString}`)
        if (res) return res

        return {
            meta: {
                page: conditions.page ?? 1,
                size: conditions.size ?? 10,
                totalCount: 0,
                totalPages: 0,
            },
            posts: [],
        }
    },
}

