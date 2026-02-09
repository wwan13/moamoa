import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react"
import { useNavigate } from "react-router-dom"
import { useQueryClient } from "@tanstack/react-query"
import { useLoginMutation, useLogoutMutation } from "../queries/auth.queries"
import { authStorageKeys, setOnLoginRequired, setOnLogout } from "../api/client"

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
    const [isLoggedIn, setIsLoggedIn] = useState(() =>
        Boolean(localStorage.getItem(authStorageKeys.accessToken))
    )

    const [sessionKey, setSessionKey] = useState(() =>
        localStorage.getItem(SESSION_KEY)
    )

    const navigate = useNavigate()
    const qc = useQueryClient()

    const loginMutation = useLoginMutation()
    const logoutMutation = useLogoutMutation()

    useEffect(() => {
        const handleLogout = async () => {
            await qc.cancelQueries()
            qc.clear()

            localStorage.removeItem(authStorageKeys.accessToken)
            localStorage.removeItem(authStorageKeys.refreshToken)
            localStorage.removeItem(SESSION_KEY)

            setIsLoggedIn(false)
            setSessionKey(null)

            navigate("/login")
        }

        setOnLogout(handleLogout)
        setOnLoginRequired(handleLogout)
    }, [qc, navigate])

    const login = async ({ email, password }: LoginParams) => {
        const res = await loginMutation.mutateAsync({ email, password })

        await qc.cancelQueries()
        qc.clear()

        localStorage.setItem(authStorageKeys.accessToken, res.accessToken)
        localStorage.setItem(authStorageKeys.refreshToken, res.refreshToken)

        const sk = newSessionKey()
        localStorage.setItem(SESSION_KEY, sk)
        setSessionKey(sk)

        setIsLoggedIn(true)
    }

    const logout = async () => {
        const res = await logoutMutation.mutateAsync()
        if (!res?.success) return

        await qc.cancelQueries()
        qc.clear()

        localStorage.removeItem(authStorageKeys.accessToken)
        localStorage.removeItem(authStorageKeys.refreshToken)
        localStorage.removeItem(SESSION_KEY)

        setIsLoggedIn(false)
        setSessionKey(null)

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
