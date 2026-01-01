import { createContext, useContext, useEffect, useState } from "react"
import { loginApi } from "../api/auth.api.js"
import { setOnLogout, showGlobalAlert, showToast } from "../api/client.js"
import {useNavigate} from "react-router-dom";

const AuthContext = createContext(null)

const ACCESS_TOKEN_KEY = "accessToken"
const REFRESH_TOKEN_KEY = "refreshToken"

export function AuthProvider({ children }) {
    const [isLoggedIn, setIsLoggedIn] = useState(() =>
        Boolean(localStorage.getItem("accessToken"))
    )

    // null | "login" | "signup"
    const [authModal, setAuthModal] = useState(null)

    const navigate = useNavigate()

    useEffect(() => {
        // setIsLoggedIn(Boolean(localStorage.getItem(ACCESS_TOKEN_KEY)))
        setOnLogout(logout)
    }, [])

    const openLogin = () => setAuthModal("login")
    const openSignup = () => setAuthModal("signup")
    const closeAuthModal = () => setAuthModal(null)

    const login = async ({ email, password }) => {
        const res = await loginApi(email, password, () => {
            showGlobalAlert("이메일 또는 비밀번호가 일치하지 않습니다.")
        })

        localStorage.setItem(ACCESS_TOKEN_KEY, res.accessToken)
        localStorage.setItem(REFRESH_TOKEN_KEY, res.refreshToken)
        setIsLoggedIn(true)

        showToast("로그인 되었습니다.")
        closeAuthModal() // ✅ 로그인 성공하면 모달 닫기
    }

    const logout = () => {
        localStorage.removeItem(ACCESS_TOKEN_KEY)
        localStorage.removeItem(REFRESH_TOKEN_KEY)
        setIsLoggedIn(false)

        navigate("/")
        showToast("로그아웃 되었습니다.")
    }

    return (
        <AuthContext.Provider
            value={{
                isLoggedIn,
                login,
                logout,

                authModal,
                openLogin,
                openSignup,
                closeAuthModal,
            }}
        >
            {children}
        </AuthContext.Provider>
    )
}

export default function useAuth() {
    return useContext(AuthContext)
}