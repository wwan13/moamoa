import styles from "./Header.module.css"
import HeaderUser from "../HeaderUser/HeaderUser"
import useAuth from "../../auth/useAuth"
import {useNavigate} from "react-router-dom";

const Header = () => {
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
                    src="https://i.imgur.com/CHYokw0.png"
                    alt="moamoa"
                    className={styles.logoImage}
                />
            </button>

            <HeaderUser
                isLoggedIn={isLoggedIn}
                onClickLogin={openLogin}
                onClickAllBlog={() => navigate("/blogs")}
                onClickMyPage={() => navigate("/my")}
            />
        </header>
    )
}
export default Header
