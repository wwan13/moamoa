import { useEffect, useState, type ReactNode } from "react"
import { useNavigate } from "react-router-dom"
import { useQueryClient } from "@tanstack/react-query"
import { useLoginMutation, useLogoutMutation } from "../queries/auth.queries"
import {
  setOnLogout,
  showGlobalAlert,
  showGlobalConfirm,
  showToast,
  authStorageKeys,
} from "../api/client"
import type { AuthTokens } from "../api/auth.api"
import {
  AuthContext,
  type AuthContextValue,
  type AuthModalType,
  type LoginParams,
  type SocialLoginParams,
} from "./context"

const SESSION_KEY = "sessionKey"

const newSessionKey = (): string => {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`
}

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(() =>
    Boolean(localStorage.getItem(authStorageKeys.accessToken))
  )

  const [sessionKey, setSessionKey] = useState<string | null>(() =>
    localStorage.getItem(SESSION_KEY)
  )

  const [authModal, setAuthModal] = useState<AuthModalType>(null)

  const navigate = useNavigate()
  const qc = useQueryClient()

  const loginMutation = useLoginMutation()
  const logoutMutation = useLogoutMutation()

  useEffect(() => {
    setOnLogout(async () => {
      await showGlobalAlert("다시 로그인해 주세요.")
      await qc.cancelQueries()
      qc.clear()

      localStorage.removeItem(authStorageKeys.accessToken)
      localStorage.removeItem(authStorageKeys.refreshToken)
      localStorage.removeItem(SESSION_KEY)

      setIsLoggedIn(false)
      setSessionKey(null)
    })
  }, [qc])

  const openLogin = (): void => setAuthModal("login")

  const closeAuthModal = (): void => setAuthModal(null)

  const openSignup = (): void => {
    closeAuthModal()
    navigate("/signup")
  }

  const login = async ({ email, password, isNew = false }: LoginParams): Promise<AuthTokens> => {
    const res = await loginMutation.mutateAsync({ email, password })

    await qc.cancelQueries()
    qc.clear()

    localStorage.setItem(authStorageKeys.accessToken, res.accessToken)
    localStorage.setItem(authStorageKeys.refreshToken, res.refreshToken)

    const sk = newSessionKey()
    localStorage.setItem(SESSION_KEY, sk)
    setSessionKey(sk)

    setIsLoggedIn(true)

    showToast(isNew ? "환영합니다." : "로그인 되었습니다.")
    closeAuthModal()
    return res
  }

  const socialLogin = async ({ accessToken, refreshToken, isNew = false }: SocialLoginParams): Promise<void> => {
    await qc.cancelQueries()
    qc.clear()

    localStorage.setItem(authStorageKeys.accessToken, accessToken)
    localStorage.setItem(authStorageKeys.refreshToken, refreshToken)

    const sk = newSessionKey()
    localStorage.setItem(SESSION_KEY, sk)
    setSessionKey(sk)

    setIsLoggedIn(true)

    showToast(isNew ? "환영합니다." : "로그인 되었습니다.")
  }

  const logout = async (): Promise<void> => {
    const ok = await showGlobalConfirm({
      message: "로그아웃 하시겠습니까?",
      cancelText: "취소",
      confirmText: "로그아웃",
    })

    if (!ok) return

    const res = await logoutMutation.mutateAsync()

    if (!res?.success) {
      await showGlobalAlert("다시 시도해 주세요")
      return
    }

    await qc.cancelQueries()
    qc.clear()

    localStorage.removeItem(authStorageKeys.accessToken)
    localStorage.removeItem(authStorageKeys.refreshToken)
    localStorage.removeItem(SESSION_KEY)

    setIsLoggedIn(false)
    setSessionKey(null)

    navigate("/")
    showToast("로그아웃 되었습니다.")
  }

  const logoutProcess = async (): Promise<void> => {
    const res = await logoutMutation.mutateAsync()

    if (!res?.success) {
      await showGlobalAlert("다시 시도해 주세요")
      return
    }

    await qc.cancelQueries()
    qc.clear()

    localStorage.removeItem(authStorageKeys.accessToken)
    localStorage.removeItem(authStorageKeys.refreshToken)
    localStorage.removeItem(SESSION_KEY)

    setIsLoggedIn(false)
    setSessionKey(null)
  }

  const authScope = isLoggedIn && sessionKey ? `auth:${sessionKey}` : null
  const publicScope = sessionKey ? `guest:${sessionKey}` : "guest:anonymous"

  const value: AuthContextValue = {
    isLoggedIn,
    sessionKey,
    authScope,
    publicScope,
    login,
    socialLogin,
    logout,
    logoutProcess,
    isLoginLoading: loginMutation.isPending,
    isLogoutLoading: logoutMutation.isPending,
    authModal,
    openLogin,
    openSignup,
    closeAuthModal,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export default AuthProvider
