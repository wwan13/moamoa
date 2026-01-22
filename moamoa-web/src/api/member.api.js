import { http } from "./client.js"

export const memberApi = {
    summary: (config) =>
        http.get("/api/member", config),

    createSocial: ({email, provider, providerKey}, config) =>
        http.post("/api/member/social", {email, provider, providerKey}, config),

    changePassword: ({oldPassword, newPassword, passwordConfirm}, config) =>
        http.patch("/api/member/password", {oldPassword, newPassword, passwordConfirm}, config),

    unjoin: (config) =>
        http.del("/api/member", config),
}