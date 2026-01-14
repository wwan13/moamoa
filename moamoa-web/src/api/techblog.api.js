// techBlog.api.js
import { http } from "./client.js"

export const techBlogApi = {
    list: (config) => http.get("/api/tech-blog", config),

    findByKey: ({ key }, config) =>
        http.get(`/api/tech-blog/${key}`, config),

    listSubscribed: (config) =>
        http.get("/api/tech-blog/subscription", config),
}