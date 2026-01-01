import { useState } from "react"
import styles from "./Header.module.css"
import HeaderUser from "../HeaderUser/HeaderUser.jsx"
import LoginModal from "../LoginModal/LoginModal.jsx"
import SignupModal from "../SignupModal/SignupModal.jsx"
import useAuth from "../../auth/AuthContext.jsx"
import {useNavigate} from "react-router-dom";

export default function Header() {
    const { isLoggedIn, login, logout, authModal, openLogin, openSignup, closeAuthModal } = useAuth()
    const [modal, setModal] = useState(null) // null | "login" | "signup"
    const navigate = useNavigate()

    return (
        <header className={styles.header}>
            <button
                type="button"
                className={styles.logoButton}
                onClick={() => navigate("/")}
                aria-label="홈으로 이동"
            >
                <img
                    src="https://i.imgur.com/nLMlIhX.png"
                    alt="moamoa"
                    className={styles.logoImage}
                />
            </button>

            <HeaderUser
                isLoggedIn={isLoggedIn}
                onClickLogin={openLogin}
                onClickLogout={logout}
                onClickMyPage={() => console.log("마이페이지")}
            />
        </header>
    )
}