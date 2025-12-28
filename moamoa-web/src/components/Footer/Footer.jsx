import styles from "./Footer.module.css"

export default function Footer() {
    return (
        <footer className={styles.footer}>
            <div className={styles.moamoa}>
                <img
                    src="https://i.imgur.com/LwlH4lc.png"
                    alt="moamoa-grey"
                    className={styles.moamoaIcon}
                />
                <p className={styles.copy}>© 2025 moamoa, All Rights Reserved.</p>
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

            <a
                href=""
                target="_blank"
                rel="noopener noreferrer"
                className={styles.link}
            >
                블로그 추가 요청
            </a>

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