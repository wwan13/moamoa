// posts.api.js
import { http } from "./client.js"

const SIZE = 20

function buildQuery(paramsObj = {}) {
    const params = new URLSearchParams()
    Object.entries(paramsObj).forEach(([k, v]) => {
        if (v === undefined || v === null || v === "") return
        params.set(k, String(v))
    })
    const q = params.toString()
    return q ? `?${q}` : ""
}

export const postsApi = {
    list: ({ page, size, query } = {}, config) => {
        const q = buildQuery({
            page: page && page > 1 ? page : undefined,
            size: size ?? SIZE,
            query: query || undefined,
        })

        return http.get(`/api/post${q}`, config)
    },

    listByTechBlogId: ({ page, techBlogId } = {}, config) => {
        const query = buildQuery({
            page: page && page > 1 ? page : undefined,
            size: SIZE,
            techBlogId: techBlogId,
        })
        return http.get(`/api/post/tech-blog${query}`, config)
    },

    listBySubscription: ({ page } = {}, config) => {
        const query = buildQuery({ page: page && page > 1 ? page : undefined, size: SIZE })
        // ✅ 네 코드가 subscribed/subscription 섞여있어서, 원래 쓰던 엔드포인트로 통일
        return http.get(`/api/post/subscribed${query}`, config)
    },

    listByBookmark: ({ page } = {}, config) => {
        const query = buildQuery({ page: page && page > 1 ? page : undefined, size: SIZE })
        return http.get(`/api/post/bookmarked${query}`, config)
    },

    increaseViewCount: ({ postId }, config) =>
        http.post(`/api/post/${postId}/view`, {}, config),
}