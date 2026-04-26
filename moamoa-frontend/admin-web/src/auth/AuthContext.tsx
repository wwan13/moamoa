import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react"
import { useNavigate } from "react-router-dom"
import { useQueryClient } from "@tanstack/react-query"
import { useLoginMutation, useLogoutMutation } from "../queries/auth.queries"
import {
  clearAuthCookieBestEffort,
  setOnLoginRequired,
  setOnLogout,
  showGlobalAlert,
} from "../api/client"
import { AuthContext, type AuthContextValue, type LoginParams } from "./context"

const SESSION_KEY = "sessionKey"

function newSessionKey() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`
}

const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [sessionKey, setSessionKey] = useState(() =>
    localStorage.getItem(SESSION_KEY),
  )
  const [isLoggedIn, setIsLoggedIn] = useState(
    () => !!localStorage.getItem(SESSION_KEY),
  )

  const navigate = useNavigate()
  const qc = useQueryClient()

  const loginMutation = useLoginMutation()
  const logoutMutation = useLogoutMutation()
  const isHandlingLoginRequiredRef = useRef(false)

  const resetSessionState = useCallback(() => {
    localStorage.removeItem(SESSION_KEY)
    setIsLoggedIn(false)
    setSessionKey(null)
  }, [])

  const startAuthenticatedSession = useCallback(() => {
    const savedSessionKey = localStorage.getItem(SESSION_KEY)
    const currentSessionKey = savedSessionKey ?? newSessionKey()
    if (!savedSessionKey) {
      localStorage.setItem(SESSION_KEY, currentSessionKey)
    }
    setSessionKey(currentSessionKey)
    setIsLoggedIn(true)
  }, [])

  useEffect(() => {
    const handleLogout = async () => {
      if (isHandlingLoginRequiredRef.current) return
      isHandlingLoginRequiredRef.current = true

      try {
        await clearAuthCookieBestEffort()
        await qc.cancelQueries()
        qc.clear()

        resetSessionState()

        navigate("/login")
      } finally {
        isHandlingLoginRequiredRef.current = false
      }
    }

    const handleLoginRequired = async () => {
      if (isHandlingLoginRequiredRef.current) return
      isHandlingLoginRequiredRef.current = true

      try {
        await clearAuthCookieBestEffort()
        await showGlobalAlert("다시 로그인해 주세요.")
        await qc.cancelQueries()
        qc.clear()

        resetSessionState()

        navigate("/login")
      } finally {
        isHandlingLoginRequiredRef.current = false
      }
    }

    setOnLogout(handleLogout)
    setOnLoginRequired(handleLoginRequired)
  }, [qc, navigate, resetSessionState])

  const login = useCallback(
    async ({ email, password }: LoginParams) => {
      await loginMutation.mutateAsync({ email, password })

      await qc.cancelQueries()
      qc.clear()

      startAuthenticatedSession()
    },
    [loginMutation, qc, startAuthenticatedSession],
  )

  const logout = useCallback(async () => {
    await logoutMutation.mutateAsync()

    await qc.cancelQueries()
    qc.clear()

    resetSessionState()

    navigate("/login")
  }, [logoutMutation, navigate, qc, resetSessionState])

  const value: AuthContextValue = useMemo(
    () => ({
      isLoggedIn,
      sessionKey,
      login,
      logout,
      isLoginLoading: loginMutation.isPending,
      isLogoutLoading: logoutMutation.isPending,
    }),
    [
      isLoggedIn,
      sessionKey,
      login,
      logout,
      loginMutation.isPending,
      logoutMutation.isPending,
    ],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export default AuthProvider
