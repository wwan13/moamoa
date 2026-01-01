import styles from "./HeaderUser.module.css"

export default function HeaderUser({ isLoggedIn, onClickLogin, onClickLogout, onClickMyPage }) {
    if (isLoggedIn) {
        return (
            <div className={styles.userActions}>
                <button className={styles.textButton} onClick={onClickMyPage}>마이페이지</button>
            </div>
        )
    }

    return (
        <button className={styles.textButton} onClick={onClickLogin}>
            로그인
        </button>
    )
}