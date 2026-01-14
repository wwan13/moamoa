import { http } from "./client.js"

export const categoryApi = {
    list: (config) => http.get("/api/category", config),
}