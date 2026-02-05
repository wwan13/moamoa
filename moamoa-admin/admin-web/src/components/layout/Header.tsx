import styles from './Header.module.css'
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined';

const Header = () => {
    return (
        <div className={styles.wrap}>
            <div className={styles.end}>
                <div
                    className={styles.iconWrap}
                    onClick={() => window.open("https://moamoa.dev")}
                >
                    <OpenInNewOutlinedIcon
                        sx={{fontSize: 24, color: "#252525"}}
                    />
                </div>
            </div>
        </div>
    )
}

export default Header