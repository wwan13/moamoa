import { http } from "./client.js" // (이전 단계에서 만든 http 래퍼 기준)

export const bookmarkApi = {
    toggle: ({ postId }, config) =>
        http.post("/api/post-bookmark", { postId }, config),
}