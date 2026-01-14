import { createContext, useContext, useEffect, useMemo, useState } from "react"
import { useNavigate } from "react-router-dom"
import { useLoginMutation, useLogoutMutation } from "../queries/auth.queries.js"
import {
    setOnLogout,
    showGlobalAlert,
    showGlobalConfirm,
    showToast,
} from "../api/client.js"

const AuthContext = createContext(null)

const ACCESS_TOKEN_KEY = "accessToken"
const REFRESH_TOKEN_KEY = "refreshToken"

export function AuthProvider({ children }) {
    const [isLoggedIn, setIsLoggedIn] = useState(() =>
        Boolean(localStorage.getItem(ACCESS_TOKEN_KEY))
    )

    // null | "login" | "signup"
    const [authModal, setAuthModal] = useState(null)

    const navigate = useNavigate()

    // ✅ React Query mutations
    const loginMutation = useLoginMutation()
    const logoutMutation = useLogoutMutation()

    useEffect(() => {
        setOnLogout(async () => {
            await showGlobalAlert("다시 로그인해 주세요.")
            localStorage.removeItem(ACCESS_TOKEN_KEY)
            localStorage.removeItem(REFRESH_TOKEN_KEY)
            setIsLoggedIn(false)
        })
    }, [])

    const openLogin = () => setAuthModal("login")
    const openSignup = () => setAuthModal("signup")
    const closeAuthModal = () => setAuthModal(null)

    // ✅ 기존처럼 await login(...) 가능하게 유지
    const login = async ({ email, password }) => {
        try {
            const res = await loginMutation.mutateAsync({ email, password })

            localStorage.setItem(ACCESS_TOKEN_KEY, res.accessToken)
            localStorage.setItem(REFRESH_TOKEN_KEY, res.refreshToken)
            setIsLoggedIn(true)

            showToast("로그인 되었습니다.")
            closeAuthModal()
            return res
        } catch (e) {
            await showGlobalAlert("이메일 또는 비밀번호가 일치하지 않습니다.")
            throw e
        }
    }

    const logout = async () => {
        const ok = await showGlobalConfirm({
            message: "로그아웃 하시겠습니까?",
            confirmText: "로그아웃",
        })
        if (!ok) return

        try {
            const res = await logoutMutation.mutateAsync()

            if (res?.success) {
                localStorage.removeItem(ACCESS_TOKEN_KEY)
                localStorage.removeItem(REFRESH_TOKEN_KEY)
                setIsLoggedIn(false)

                navigate("/")
                showToast("로그아웃 되었습니다.")
            } else {
                await showGlobalAlert("다시 시도해 주세요")
            }
        } catch (e) {
            await showGlobalAlert("다시 시도해 주세요")
            throw e
        }
    }

    const value = useMemo(
        () => ({
            isLoggedIn,

            // 기존 API
            login,
            logout,

            // 로딩 상태도 전역에서 노출 (원하면 컴포넌트가 사용 가능)
            isLoginLoading: loginMutation.isPending,
            isLogoutLoading: logoutMutation.isPending,

            authModal,
            openLogin,
            openSignup,
            closeAuthModal,
        }),
        [
            isLoggedIn,
            authModal,
            loginMutation.isPending,
            logoutMutation.isPending,
        ]
    )

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export default function useAuth() {
    return useContext(AuthContext)
}