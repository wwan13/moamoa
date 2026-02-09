import styles from "./Footer.module.css"
import {useNavigate} from "react-router-dom";
import {showGlobalConfirm} from "../../api/client";
import useAuth from "../../auth/useAuth";

const Footer = () => {
    const navigate = useNavigate()
    const {isLoggedIn, openLogin} = useAuth()

    const handleSubmissionButton = async () => {
        if (!isLoggedIn) {
            const ok = await showGlobalConfirm({
                title : "로그인",
                message : "로그인이 필요한 기능입니다. 로그인 하시겠습니까?",
                confirmText : "로그인"
            })
            if (!ok) {
                return
            }
            openLogin()
            return
        }

        navigate("/submission")
    }

    return (
        <footer className={styles.footer}>
            <div className={styles.moamoa}>
                {/*<img*/}
                {/*    src="https://i.imgur.com/Sjc8OID.png"*/}
                {/*    alt="moamoa-grey"*/}
                {/*    className={styles.moamoaIcon}*/}
                {/*/>*/}
                <p className={styles.copy}>© 2026 moamoa, All Rights Reserved.</p>
            </div>
            <a
                href=""
                target="_blank"
                rel="noopener noreferrer"
                className={styles.link}
            >
                공지사항
            </a>

            <span>·</span>

            <span
                rel="noopener noreferrer"
                className={styles.link}
                onClick={handleSubmissionButton}
            >
                블로그 추가 요청
            </span>

            <span>·</span>

            <a
                href=""
                target="_blank"
                rel="noopener noreferrer"
                className={styles.link}
            >
                개인정보 처리방침
            </a>

            <span>·</span>

            <a
                href="https://github.com/wwan13/moamoa"
                target="_blank"
                rel="noopener noreferrer"
                className={styles.link}
            >
                저장소
            </a>

            <span>·</span>

            <a
                href="https://github.com/wwan13"
                target="_blank"
                rel="noopener noreferrer"
                className={styles.link}
            >
                깃허브
            </a>

            <span>·</span>

            <a
                href="https://www.linkedin.com/in/wwan13"
                target="_blank"
                rel="noopener noreferrer"
                className={styles.link}
            >
                링크드인
            </a>

            <span>·</span>

            <a
                href="mailto:wwan13@naver.com"
                target="_blank"
                rel="noopener noreferrer"
                className={styles.link}
            >
                문의
            </a>
        </footer>
    )
}
export default Footer
