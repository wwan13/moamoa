import { http } from "./client.js"

export const techBlogSubscriptionApi = {
    list: (config) => http.get("/api/tech-blog-subscription", config),

    toggleSubscription: ({ techBlogId }, config) =>
        http.post("/api/tech-blog-subscription", { techBlogId }, config),

    toggleNotification: ({ techBlogId }, config) =>
        http.patch("/api/tech-blog-subscription/notification-enabled", { techBlogId }, config),
}