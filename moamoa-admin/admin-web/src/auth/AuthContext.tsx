import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react"
import { useNavigate } from "react-router-dom"
import { useQueryClient } from "@tanstack/react-query"
import { useLoginMutation, useLogoutMutation } from "../queries/auth.queries"
import { setOnLoginRequired, setOnLogout } from "../api/client"

type LoginParams = {
    email: string
    password: string
}

type AuthContextValue = {
    isLoggedIn: boolean
    sessionKey: string | null
    login: (params: LoginParams) => Promise<void>
    logout: () => Promise<void>
    isLoginLoading: boolean
    isLogoutLoading: boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

const SESSION_KEY = "sessionKey"

function newSessionKey() {
    return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`
}

export function AuthProvider({ children }: { children: ReactNode }) {
    const [sessionKey, setSessionKey] = useState(() =>
        localStorage.getItem(SESSION_KEY)
    )
    const [isLoggedIn, setIsLoggedIn] = useState(() => !!localStorage.getItem(SESSION_KEY))

    const navigate = useNavigate()
    const qc = useQueryClient()

    const loginMutation = useLoginMutation()
    const logoutMutation = useLogoutMutation()

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
            await qc.cancelQueries()
            qc.clear()

            resetSessionState()

            navigate("/login")
        }

        setOnLogout(handleLogout)
        setOnLoginRequired(handleLogout)
    }, [qc, navigate, resetSessionState])

    const login = async ({ email, password }: LoginParams) => {
        await loginMutation.mutateAsync({ email, password })

        await qc.cancelQueries()
        qc.clear()

        startAuthenticatedSession()
    }

    const logout = async () => {
        const res = await logoutMutation.mutateAsync()
        if (!res?.success) return

        await qc.cancelQueries()
        qc.clear()

        resetSessionState()

        navigate("/login")
    }

    const value = useMemo(
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
            loginMutation.isPending,
            logoutMutation.isPending,
            startAuthenticatedSession,
        ]
    )

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

const useAuth = () => {
    const ctx = useContext(AuthContext)
    if (!ctx) throw new Error("useAuth must be used within AuthProvider")
    return ctx
}

export default useAuth
