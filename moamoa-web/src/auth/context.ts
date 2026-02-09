import { createContext } from "react"
import type { AuthTokens, LoginCommand } from "../api/auth.api"

export type AuthModalType = "login" | "signup" | null

export type LoginParams = LoginCommand & {
  isNew?: boolean
}

export type SocialLoginParams = AuthTokens & {
  isNew?: boolean
}

export type AuthContextValue = {
  isLoggedIn: boolean
  sessionKey: string | null
  authScope: string | null
  publicScope: string
  login: (params: LoginParams) => Promise<AuthTokens>
  socialLogin: (params: SocialLoginParams) => Promise<void>
  logout: () => Promise<void>
  logoutProcess: () => Promise<void>
  isLoginLoading: boolean
  isLogoutLoading: boolean
  authModal: AuthModalType
  openLogin: () => void
  openSignup: () => void
  closeAuthModal: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
