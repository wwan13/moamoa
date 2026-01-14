import styles from "./Header.module.css"
import HeaderUser from "../HeaderUser/HeaderUser.jsx"
import useAuth from "../../auth/AuthContext.jsx"
import {useNavigate} from "react-router-dom";

export default function Header() {
    const { isLoggedIn, openLogin } = useAuth()
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
                    src="https://i.imgur.com/zbgOugR.png"
                    alt="moamoa"
                    className={styles.logoImage}
                />
            </button>

            <HeaderUser
                isLoggedIn={isLoggedIn}
                onClickLogin={openLogin}
                onCLickAllBLog={() => navigate("/blogs")}
                onClickMyPage={() => navigate("/my")}
            />
        </header>
    )
}