import styles from "./MyPage.module.css"
import useAuth from "../../auth/AuthContext.jsx";
import {useEffect} from "react";
import {useNavigate} from "react-router-dom";
import ArrowForwardIosIcon from '@mui/icons-material/ArrowForwardIos';
import GlobalSpinner from "../../components/GlobalSpinner/GlobalSpinner.jsx";
import BookmarkIcon from "@mui/icons-material/Bookmark";
import NoteIcon from '@mui/icons-material/Note';
import LocalOfferIcon from '@mui/icons-material/LocalOffer';

export default function MyPage() {
    const { isLoggedIn, logout, isLogoutLoading } = useAuth()
    const navigate = useNavigate()

    useEffect(() => {
        if (!isLoggedIn) {
            navigate("/")
        }
    }, [isLoggedIn]);

    return (
        <div className={styles.wrap}>
            { isLogoutLoading && <GlobalSpinner /> }
            <div>
                <p className={styles.title}>ddangddo0511</p>
                <div className={styles.stats}>
                    <div className={styles.stat}>
                        <div className={styles.statTitle}>
                            <span className={styles.statLabel}>구독</span>
                            <LocalOfferIcon
                                className={styles.statIcon}
                                sx={{fontSize: 26, color: "#3B4953", fontWeight: 800}}
                            />
                        </div>
                        <span className={styles.statValue}>8</span>
                    </div>
                    <div className={styles.stat}>
                        <div className={styles.statTitle}>
                            <span className={styles.statLabel}>북마크</span>
                            <BookmarkIcon
                                className={styles.statIcon}
                                sx={{fontSize: 26, color: "#90AB8B", fontWeight: 800}}
                            />
                        </div>
                        <span className={styles.statValue}>8</span>
                    </div>
                </div>
            </div>

            <section className={styles.section}>
                <p className={styles.sectionTitle}>활동</p>
                <div className={styles.buttons}>
                    <button className={styles.button} onClick={() => navigate("/subscription")}>
                        <p className={styles.buttonText}>모든 구독 블로그</p>
                        <ArrowForwardIosIcon sx={{ fontSize: 16, color: "#A8A8A8" }}/>
                    </button>
                    <div className={styles.divider}/>
                    <button className={styles.button} onClick={() => console.log("asd")}>
                        <p className={styles.buttonText}>블로그 요청 내역</p>
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