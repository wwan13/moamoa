// techBlog.api.js
import { http } from "./client.js"

function buildQuery(paramsObj = {}) {
    const params = new URLSearchParams()
    Object.entries(paramsObj).forEach(([k, v]) => {
        if (v === undefined || v === null || v === "") return
        params.set(k, String(v))
    })
    const q = params.toString()
    return q ? `?${q}` : ""
}

export const techBlogApi = {
    list: ({ query } = {}, config) => {
        const q = buildQuery({
            query: query || undefined,
        })
        return http.get(`/api/tech-blog${q}`, config)
    },

    findByKey: ({ key }, config) =>
        http.get(`/api/tech-blog/${key}`, config),

    listSubscribed: (config) =>
        http.get("/api/tech-blog/subscription", config),
}
