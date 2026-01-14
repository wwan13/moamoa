import { useEffect, useState } from "react"
import styles from "./LeftSidebar.module.css"
import Subscriptions from "../Subscriptions/Subscriptions.jsx"
import ArrowForwardIosIcon from "@mui/icons-material/ArrowForwardIos"

const SKELETON_DELAY_MS = 1000

export default function LeftSidebar({
                                        subscriptions = [],
                                        type, // "all" | "subscribed" | "bookmarked"
                                        blogKey, // string | null
                                        onSelectType,
                                        onSelectBlog,
                                        isLoading = false, // 조회 로딩
                                    }) {
    const isAllActive = type === "all"
    const isBookmarkedActive = type === "bookmarked"
    const isSubscribedTabActive = type === "subscribed" && !blogKey

    // ✅ 1초 이상일 때만 로딩 상태 전파
    const [showSkeleton, setShowSkeleton] = useState(false)

    useEffect(() => {
        let timer = null
        if (isLoading) {
            timer = setTimeout(() => setShowSkeleton(true), SKELETON_DELAY_MS)
        } else {
            setShowSkeleton(false)
        }
        return () => timer && clearTimeout(timer)
    }, [isLoading])

    const disabled = showSkeleton

    return (
        <aside className={styles.wrap}>
            <button
                type="button"
                className={`${styles.header} ${isAllActive ? styles.active : ""}`}
                onClick={() => onSelectType("all")}
                disabled={disabled}
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
                isLoading={showSkeleton}
            />

            <button
                type="button"
                className={`${styles.header} ${isBookmarkedActive ? styles.active : ""}`}
                onClick={() => onSelectType("bookmarked")}
                disabled={disabled}
            >
                <span className={styles.title}>북마크</span>
                <ArrowForwardIosIcon sx={{ fontSize: 14, color: "#252525" }} />
            </button>
        </aside>
    )
}