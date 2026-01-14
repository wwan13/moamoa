import { useEffect, useState } from "react"
import styles from "./CategoryTabs.module.css"
import ArrowForwardIosIcon from "@mui/icons-material/ArrowForwardIos"

const SKELETON_DELAY_MS = 1000

export default function CategoryTabs({
                                         items = [],
                                         id,
                                         onChange,
                                         isSubscribing = false,
                                         onClickSubscriptions,
                                         isLoading = false, // query 로딩
                                     }) {
    // ✅ 1초 이상일 때만 스켈레톤 표시
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

    return (
        <div className={styles.wrap}>
            <nav className={styles.tab} aria-label="카테고리">
                <div className={styles.tabList}>
                    {showSkeleton
                        ? Array.from({ length: 5 }).map((_, i) => (
                            <div key={i} className={`${styles.tabItem} ${styles.skeleton}`} />
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