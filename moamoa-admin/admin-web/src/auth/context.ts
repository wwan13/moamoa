import { createContext } from "react"

export type LoginParams = {
  email: string
  password: string
}

export type AuthContextValue = {
  isLoggedIn: boolean
  sessionKey: string | null
  login: (params: LoginParams) => Promise<void>
  logout: () => Promise<void>
  isLoginLoading: boolean
  isLogoutLoading: boolean
}

export const AuthContext = createContext<AuthContextValue | null>(null)
