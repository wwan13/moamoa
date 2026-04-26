import { http } from "./client"

export type AdminLoginCommand = {
  email: string
  password: string
}

export type AdminAuthTokens = {
  accessToken: string
  refreshToken: string
}

export const authApi = {
  login: async (command: AdminLoginCommand): Promise<AdminAuthTokens> => {
    const res = await http.post<AdminAuthTokens>(
      "/api/admin/auth/login",
      command,
    )
    if (!res) throw new Error("EMPTY_LOGIN_RESPONSE")
    return res
  },
  logout: async (): Promise<void> => {
    await http.post<void>("/api/admin/auth/logout", {})
  },
}
