import { http } from "./client.js"

export const authApi = {
    login: ({ email, password }, config) =>
        http.post("/api/auth/login", { email, password }, config),

    signup: ({ email, password, passwordConfirm }, config) =>
        http.post("/api/member", { email, password, passwordConfirm }, config),

    logout: (config) =>
        http.post("/api/auth/logout", {}, config),

    loginSocialSession: ({ token, memberId }, config) =>
        http.post("/api/auth/login/social", { token, memberId }, config),
}