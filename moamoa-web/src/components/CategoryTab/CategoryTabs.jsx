import styles from "./CategoryTabs.module.css"
import ArrowForwardIosIcon from "@mui/icons-material/ArrowForwardIos"

export default function CategoryTabs({
                                         items,
                                         id,
                                         onChange,
                                         isSubscribing = false,
                                         onClickSubscriptions,
                                     }) {
    return (
        <div className={styles.wrap}>
            <nav className={styles.tab} aria-label="카테고리">
                <div className={styles.tabList}>
                    {items.map((it) => {
                        const active = it.id === id
                        return (
                            <button
                                key={it.id}
                                type="button"
                                className={`${styles.tabItem} ${active ? styles.active : ""}`}
                                onClick={() => onChange(it.id)}
                                aria-current={active ? "page" : undefined}
                            >
                                {it.title}
                            </button>
                        )
                    })}
                </div>

                {isSubscribing && (
                    <button
                        type="button"
                        className={styles.subscriptions}
                        onClick={onClickSubscriptions}
                    >
                        <span>모든 구독 블로그</span>
                        <ArrowForwardIosIcon sx={{ fontSize: 11, color: "#6b7280" }} />
                    </button>
                )}
            </nav>
        </div>
    )
}