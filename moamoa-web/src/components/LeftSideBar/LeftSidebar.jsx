import styles from "./LeftSidebar.module.css"
import Subscriptions from "../Subscriptions/Subscriptions.jsx"
import ArrowForwardIosIcon from "@mui/icons-material/ArrowForwardIos"

export default function LeftSidebar({
                                        subscriptions,
                                        type,        // "all" | "subscribed" | "bookmarked"
                                        blogKey,     // string | null
                                        onSelectType,
                                        onSelectBlog,
                                    }) {
    const isAllActive = type === "all"
    const isBookmarkedActive = type === "bookmarked"

    // ✅ 블로그 선택되면 구독 "탭"은 active 아님
    const isSubscribedTabActive = type === "subscribed" && !blogKey

    return (
        <aside className={styles.wrap}>
            <button
                type="button"
                className={`${styles.header} ${isAllActive ? styles.active : ""}`}
                onClick={() => onSelectType("all")}
            >
                <span className={styles.title}>전체</span>
                <ArrowForwardIosIcon sx={{ fontSize: 14, color: "#252525" }} />
            </button>

            <Subscriptions
                items={subscriptions}
                headerActive={isSubscribedTabActive}
                activeBlogKey={blogKey}
                onClickHeader={() => onSelectType("subscribed")}
                onClickItem={(item) => onSelectBlog(item.key)}
            />

            <button
                type="button"
                className={`${styles.header} ${isBookmarkedActive ? styles.active : ""}`}
                onClick={() => onSelectType("bookmarked")}
            >
                <span className={styles.title}>북마크</span>
                <ArrowForwardIosIcon sx={{ fontSize: 14, color: "#252525" }} />
            </button>
        </aside>
    )
}