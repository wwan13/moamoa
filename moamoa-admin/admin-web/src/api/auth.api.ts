import { http } from "./client"

export type AdminLoginCommand = {
    email: string
    password: string
}

export type AdminAuthTokens = {
    accessToken: string
    refreshToken: string
}

export type AdminLogoutResult = {
    success: boolean
}

export const authApi = {
    login: async (command: AdminLoginCommand): Promise<AdminAuthTokens> => {
        const res = await http.post<AdminAuthTokens>("/api/admin/auth/login", command)
        if (!res) throw new Error("EMPTY_LOGIN_RESPONSE")
        return res
    },
    logout: async (): Promise<AdminLogoutResult> => {
        const res = await http.post<AdminLogoutResult>("/api/admin/auth/logout", {})
        return res ?? { success: false }
    },
}
