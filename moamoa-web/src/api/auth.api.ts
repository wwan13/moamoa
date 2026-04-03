import { http, type ApiRequestConfig } from "./client"

export type LoginCommand = {
  email: string
  password: string
}

export type SignupCommand = {
  email: string
  password: string
  passwordConfirm: string
}

export type LoginSocialSessionCommand = {
  token: string
  memberId: number
}

export type AuthTokens = {
  accessToken: string
  refreshToken: string
}

export const authApi = {
  login: async (
    command: LoginCommand,
    config?: ApiRequestConfig,
  ): Promise<AuthTokens> => {
    const res = await http.post<AuthTokens>("/api/auth/login", command, config)
    if (!res) throw new Error("EMPTY_LOGIN_RESPONSE")
    return res
  },
  signup: async (
    command: SignupCommand,
    config?: ApiRequestConfig,
  ): Promise<AuthTokens> => {
    const res = await http.post<AuthTokens>("/api/member", command, config)
    if (!res) throw new Error("EMPTY_SIGNUP_RESPONSE")
    return res
  },
  logout: async (config?: ApiRequestConfig): Promise<void> => {
    await http.post<void>("/api/auth/logout", {}, config)
  },
  loginSocialSession: async (
    command: LoginSocialSessionCommand,
    config?: ApiRequestConfig,
  ): Promise<AuthTokens> => {
    const res = await http.post<AuthTokens>(
      "/api/auth/login/social",
      command,
      config,
    )
    if (!res) throw new Error("EMPTY_SOCIAL_LOGIN_RESPONSE")
    return res
  },
}
