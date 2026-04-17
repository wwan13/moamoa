import { http, type ApiRequestConfig } from "./client"

export type Provider = "INTERNAL" | "GOOGLE" | "GITHUB"

export type MemberData = {
  id: number
  email: string
  role: "USER" | "ADMIN"
}

export type MemberSummary = {
  memberId: number
  email: string
  provider: Provider
  subscribeCount: number
  bookmarkCount: number
}

export type CreateSocialMemberCommand = {
  email: string
  provider: Provider
  providerKey: string
}

export type CreateSocialMemberResult = {
  member: MemberData
  token: string
}

export type ChangePasswordCommand = {
  oldPassword: string
  newPassword: string
  passwordConfirm: string
}

export type ApplyTemporaryPasswordCommand = {
  email: string
}

export const memberApi = {
  summary: async (config?: ApiRequestConfig): Promise<MemberSummary | null> => {
    return await http.get<MemberSummary>("/api/member", config)
  },
  createSocial: async (
    command: CreateSocialMemberCommand,
    config?: ApiRequestConfig,
  ): Promise<CreateSocialMemberResult> => {
    const res = await http.post<CreateSocialMemberResult>(
      "/api/member/social",
      command,
      config,
    )
    if (!res) throw new Error("EMPTY_SOCIAL_MEMBER_RESPONSE")
    return res
  },
  changePassword: async (
    command: ChangePasswordCommand,
    config?: ApiRequestConfig,
  ): Promise<void> => {
    await http.post<void>("/api/member/password", command, config)
  },
  applyTemporaryPassword: async (
    command: ApplyTemporaryPasswordCommand,
    config?: ApiRequestConfig,
  ): Promise<void> => {
    await http.post<void>("/api/member/password/temporary", command, config)
  },
  unjoin: async (config?: ApiRequestConfig): Promise<void> => {
    await http.del<void>("/api/member", config)
  },
}
