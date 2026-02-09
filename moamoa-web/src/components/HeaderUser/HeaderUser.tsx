import styles from "./HeaderUser.module.css"
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import { openSearch } from "../../api/client"

type HeaderUserProps = {
    isLoggedIn: boolean
    onClickLogin: () => void
    onClickAllBlog: () => void
    onClickMyPage: () => void
}

const HeaderUser = ({ isLoggedIn, onClickLogin, onClickAllBlog, onClickMyPage }: HeaderUserProps) => {
    if (isLoggedIn) {
        return (
            <div className={styles.userActions}>
                <button className={styles.textButton} onClick={onClickAllBlog}>모든 블로그</button>
                <button className={styles.textButton} onClick={onClickMyPage}>마이페이지</button>
                <button className={styles.textButton} onClick={openSearch}>
                    <SearchOutlinedIcon sx={{fontSize: 20}}/>
                </button>
            </div>
        )
    }

    return (
        <div className={styles.userActions}>
            <button className={styles.textButton} onClick={onClickAllBlog}>모든 블로그</button>
            <button className={styles.textButton} onClick={onClickLogin}>로그인</button>
            <button className={styles.textButton} onClick={openSearch}>
                <SearchOutlinedIcon sx={{fontSize: 20}}/>
            </button>
        </div>

    )
}
export default HeaderUser
