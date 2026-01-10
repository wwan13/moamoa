import styles from "./MyPage.module.css"
import useAuth from "../../auth/AuthContext.jsx";
import {useEffect} from "react";
import {useNavigate} from "react-router-dom";
import ArrowForwardIosIcon from '@mui/icons-material/ArrowForwardIos';

export default function MyPage() {
    const { isLoggedIn, logout } = useAuth()
    const navigate = useNavigate()

    useEffect(() => {
        if (!isLoggedIn) {
            navigate("/")
        }
    }, [isLoggedIn]);

    return (
        <div className={styles.wrap}>
            <section className={styles.section}>
                <p className={styles.sectionTitle}>활동</p>
                <div className={styles.buttons}>
                    <button className={styles.button}>
                        <p className={styles.buttonText}>구독 관리</p>
                        <ArrowForwardIosIcon sx={{ fontSize: 16, color: "#A8A8A8" }}/>
                    </button>
                </div>
            </section>

            <section className={styles.section}>
                <p className={styles.sectionTitle}>설정</p>
                <div className={styles.buttons}>
                    <button className={styles.button}>
                        <p className={styles.buttonText}>알림 설정</p>
                        <ArrowForwardIosIcon sx={{ fontSize: 16, color: "#A8A8A8" }}/>
                    </button>
                    <div className={styles.divider}/>
                    <button className={styles.button}>
                        <p className={styles.buttonText}>비밀번호 설정</p>
                        <ArrowForwardIosIcon sx={{ fontSize: 16, color: "#A8A8A8" }}/>
                    </button>
                    <div className={styles.divider}/>
                    <button className={styles.button}>
                        <p className={styles.buttonText}>회원 탈퇴</p>
                        <ArrowForwardIosIcon sx={{ fontSize: 16, color: "#A8A8A8" }}/>
                    </button>
                </div>
            </section>

            <button
                className={styles.logoutButton}
                onClick={logout}
            >로그아웃</button>
        </div>
    )
}