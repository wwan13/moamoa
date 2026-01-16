import { createContext, useContext, useEffect, useMemo, useState } from "react"
import { useNavigate } from "react-router-dom"
import { useLoginMutation, useLogoutMutation } from "../queries/auth.queries.js"
import {
    setOnLogout,
    showGlobalAlert,
    showGlobalConfirm,
    showToast,
} from "../api/client.js"
import { useQueryClient } from "@tanstack/react-query"

const AuthContext = createContext(null)

const ACCESS_TOKEN_KEY = "accessToken"
const REFRESH_TOKEN_KEY = "refreshToken"
const SESSION_KEY = "sessionKey"

function newSessionKey() {
    return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`
}

export function AuthProvider({ children }) {
    const [isLoggedIn, setIsLoggedIn] = useState(() =>
        Boolean(localStorage.getItem(ACCESS_TOKEN_KEY))
    )

    // 세션 기준 캐시 분리용 키
    const [sessionKey, setSessionKey] = useState(() =>
        localStorage.getItem(SESSION_KEY)
    )

    // null | "login" | "signup"
    const [authModal, setAuthModal] = useState(null)

    const navigate = useNavigate()
    const qc = useQueryClient()

    const loginMutation = useLoginMutation()
    const logoutMutation = useLogoutMutation()

    useEffect(() => {
        setOnLogout(async () => {
            await showGlobalAlert("다시 로그인해 주세요.")
            await qc.cancelQueries()
            qc.clear()

            localStorage.removeItem(ACCESS_TOKEN_KEY)
            localStorage.removeItem(REFRESH_TOKEN_KEY)
            localStorage.removeItem(SESSION_KEY)

            setIsLoggedIn(false)
            setSessionKey(null)
        })
    }, [qc])

    const openLogin = () => setAuthModal("login")
    const openSignup = () => {
        closeAuthModal()
        navigate("/signup")
    }
    const closeAuthModal = () => setAuthModal(null)

    const login = async ({ email, password, isNew = false }) => {
        const res = await loginMutation.mutateAsync({ email, password })

        await qc.cancelQueries()
        qc.clear()

        localStorage.setItem(ACCESS_TOKEN_KEY, res.accessToken)
        localStorage.setItem(REFRESH_TOKEN_KEY, res.refreshToken)

        const sk = newSessionKey()
        localStorage.setItem(SESSION_KEY, sk)
        setSessionKey(sk)

        setIsLoggedIn(true)

        showToast(isNew ? "환영합니다." : "로그인 되었습니다.")
        closeAuthModal()
        return res
    }

    const socialLogin = async ({ accessToken, refreshToken, isNew = false }) => {
        await qc.cancelQueries()
        qc.clear()

        localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
        localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)

        const sk = newSessionKey()
        localStorage.setItem(SESSION_KEY, sk)
        setSessionKey(sk)

        setIsLoggedIn(true)

        showToast(isNew ? "환영합니다." : "로그인 되었습니다.")
    }

    const logout = async () => {
        const ok = await showGlobalConfirm({
            message: "로그아웃 하시겠습니까?",
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

        localStorage.removeItem(ACCESS_TOKEN_KEY)
        localStorage.removeItem(REFRESH_TOKEN_KEY)
        localStorage.removeItem(SESSION_KEY)

        setIsLoggedIn(false)
        setSessionKey(null)

        navigate("/")
        showToast("로그아웃 되었습니다.")
    }

    const value = useMemo(() => {
        const authScope =
            isLoggedIn && sessionKey ? `auth:${sessionKey}` : null

        const publicScope = sessionKey
            ? `guest:${sessionKey}`
            : "guest:anonymous"

        return {
            // 상태
            isLoggedIn,
            sessionKey,

            // ✅ 공식 scope
            authScope,     // 로그인 전용
            publicScope,   // 항상 사용 가능

            // 액션
            login,
            socialLogin,
            logout,

            // 로딩
            isLoginLoading: loginMutation.isPending,
            isLogoutLoading: logoutMutation.isPending,

            // 모달
            authModal,
            openLogin,
            openSignup,
            closeAuthModal,
        }
    }, [
        isLoggedIn,
        sessionKey,
        authModal,
        loginMutation.isPending,
        logoutMutation.isPending,
    ])

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export default function useAuth() {
    return useContext(AuthContext)
}