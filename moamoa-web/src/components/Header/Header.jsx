import { useState } from "react"
import styles from "./Header.module.css"
import HeaderUser from "../HeaderUser/HeaderUser.jsx"
import LoginModal from "../LoginModal/LoginModal.jsx"
import SignupModal from "../SignupModal/SignupModal.jsx"
import useAuth from "../../auth/AuthContext.jsx"

export default function Header() {
    const { isLoggedIn, login, logout } = useAuth()
    const [modal, setModal] = useState(null) // null | "login" | "signup"

    return (
        <header className={styles.header}>
            <button
                type="button"
                className={styles.logoButton}
                onClick={() => console.log("home")}
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
                onClickLogin={() => setModal("login")}
                onClickLogout={() => logout()}
                onClickMyPage={() => console.log("마이페이지")}
            />

            <LoginModal
                open={modal === "login"}
                onClose={() => setModal(null)}
                onSubmit={async (form) => {
                    await login(form)
                    setModal(null)
                }}
                onClickSignup={() => setModal("signup")}
            />

            <SignupModal
                open={modal === "signup"}
                onClose={() => setModal(null)}
                onClickLogin={() => setModal("login")}
            />
        </header>
    )
}