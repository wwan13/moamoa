import styles from "./CategoryTabs.module.css"
import ArrowForwardIosIcon from "@mui/icons-material/ArrowForwardIos"

type CategoryTabItem = {
    id: number
    key: string
    title: string
}

type CategoryTabsProps = {
    items?: CategoryTabItem[]
    id: number
    onChange: (nextId: number) => void
    isSubscribing?: boolean
    onClickSubscriptions?: () => void
    isLoading?: boolean
}

const CategoryTabs = ({
    items = [],
    id,
    onChange,
    isSubscribing = false,
    onClickSubscriptions,
    isLoading = false,
}: CategoryTabsProps) => {

    return (
        <div className={styles.wrap}>
            <nav className={styles.tab} aria-label="카테고리">
                <div className={styles.tabList}>
                    {isLoading
                        ? Array.from({ length: 6 }).map((_, i) => (
                            <div key={i} className={`${styles.skeletonTab}`} />
                        ))
                        : items.map((it) => {
                            const active = it.id === id
                            return (
                                <button
                                    key={it.id}
                                    type="button"
                                    className={`${styles.tabItem} ${active ? styles.active : ""}`}
                                    onClick={() => onChange(it.id)}
                                    aria-current={active ? "page" : undefined}
                                    disabled={isLoading} // 1초 전까지는 기존 탭 클릭 허용할지 선택인데, 안전하게 막음
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
                        disabled={isLoading}
                    >
                        <span>모든 구독 블로그</span>
                        <ArrowForwardIosIcon sx={{ fontSize: 11, color: "#6b7280" }} />
                    </button>
                )}
            </nav>
        </div>
    )
}
export default CategoryTabs
