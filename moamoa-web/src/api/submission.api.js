import { http } from "./client.js"

export const submissionApi = {
    create: ({ blogTitle, blogUrl, notificationEnabled }, config) =>
        http.post("/api/submission", { blogTitle, blogUrl, notificationEnabled }, config),
}