import { http } from "./client.js"

export const authApi = {
    login: ({ email, password }, config) =>
        http.post("/api/auth/login", { email, password }, config),

    emailVerification: ({ email }, config) =>
        http.post("/api/auth/email-verification", { email }, config),

    emailVerificationConfirm: ({ email, code }, config) =>
        http.post(
            "/api/auth/email-verification/confirm",
            { email, code: String(code) },
            config
        ),

    signup: ({ email, password }, config) =>
        http.post("/api/auth/signup", { email, password }, config),

    logout: (config) =>
        http.post("/api/auth/logout", {}, config),
}