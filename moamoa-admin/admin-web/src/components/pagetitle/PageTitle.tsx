import styles from './PageTitle.module.css'
import ArrowBackIosOutlinedIcon from '@mui/icons-material/ArrowBackIosOutlined';

type PageTitleProps = {
    value: string
    historyBack?: boolean
}

const PageTitle = ({value, historyBack = false}: PageTitleProps) => {
    return (
        <div className={styles.wrap}>
            {historyBack && (
                <button
                    className={styles.historyBack}
                    onClick={() => window.history.back()}
                >
                    <ArrowBackIosOutlinedIcon
                        sx={{fontSize: 20, color: "#252525"}}
                    />
                </button>
            )}
            <h3 className={styles.title}>{value}</h3>
        </div>
    )
}

export default PageTitle