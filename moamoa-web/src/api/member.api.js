import { http } from "./client.js"

export const memberApi = {
    summary: (config) =>
        http.get("/api/member", config),
}