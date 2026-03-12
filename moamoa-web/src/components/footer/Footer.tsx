import styles from "./Footer.module.css"
import {useNavigate} from "react-router-dom";
import {showGlobalConfirm} from "../../api/client";
import useAuth from "../../auth/useAuth";
import LinkedInIcon from '@mui/icons-material/LinkedIn';
import GitHubIcon from '@mui/icons-material/GitHub';
import {SvgIcon} from "@mui/material";

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
            <div className={styles.footerContent}>
                <div className={styles.contentTop}>
                    <img src="/moamoa_main_logo.png" alt="모아모아" className={styles.topLogo}/>
                    <div className={styles.topLink}>
                        <button className={styles.link}>
                            공지사항
                        </button>
                        <button
                            className={`${styles.bold} ${styles.link}`}
                            onClick={() => {navigate("/privacy")}}
                        >
                            개인정보 처리방침
                        </button>
                        <button
                            className={styles.link}
                            onClick={handleSubmissionButton}
                        >
                            블로그 추가 요청
                        </button>
                        <button className={styles.link}>
                            문의 및 피드백
                        </button>
                    </div>
                </div>
                <div className={styles.divider} />
                <div className={styles.contentBottom}>
                    <p className={styles.bottomCopy}>Copyright ⓒ moamoa All Rights Reserved.</p>

                    <div className={styles.bottomIcon}>
                        <a href="https://www.linkedin.com/in/wwan13/"
                           target="_blank" rel="noopener noreferrer"
                           className={`${styles.bgLinkedIn} ${styles.icon}`}
                        >
                            <LinkedInIcon
                                sx={{fontSize: 36, color: "#252525"}}
                            />
                        </a>
                        <a href="https://github.com/wwan13"
                           target="_blank" rel="noopener noreferrer"
                           className={`${styles.bgGithub} ${styles.icon}`}
                        >
                            <GitHubIcon
                                sx={{fontSize: 36, color: "#252525"}}
                            />
                        </a>
                    </div>
                </div>
            </div>
        </footer>
    )
}
export default Footer
