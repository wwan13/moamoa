import styles from "./HeaderUser.module.css"
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';

export default function HeaderUser({ isLoggedIn, onClickLogin, onCLickAllBLog, onClickMyPage }) {
    if (isLoggedIn) {
        return (
            <div className={styles.userActions}>
                <button className={styles.textButton} onClick={onCLickAllBLog}>모든 블로그</button>
                <button className={styles.textButton} onClick={onClickMyPage}>마이페이지</button>
                {/*<button className={styles.textButton} onClick={onClickMyPage}>*/}
                {/*    <SearchOutlinedIcon fontSize="medium"/>*/}
                {/*</button>*/}
            </div>
        )
    }

    return (
        <div className={styles.userActions}>
            <button className={styles.textButton} onClick={onCLickAllBLog}>모든 블로그</button>
            <button className={styles.textButton} onClick={onClickLogin}>로그인</button>
        </div>

    )
}