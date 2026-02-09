import { useMemo, useState } from "react"
import styles from "./Subscriptions.module.css"
import ExpandMoreIcon from "@mui/icons-material/ExpandMore"
import ArrowForwardIosIcon from "@mui/icons-material/ArrowForwardIos"

type SubscriptionItem = {
    id: number | string
    key?: string
    icon: string
    title: string
}

type SubscriptionsProps = {
    items?: SubscriptionItem[]
    maxVisible?: number
    onClickItem?: (item: SubscriptionItem) => void
    onClickHeader?: () => void
    headerActive?: boolean
    activeBlogKey?: number | string | null
    isLoading?: boolean
}

const Subscriptions = ({
    items,
    maxVisible = 5,
    onClickItem,
    onClickHeader,
    headerActive = false,
    activeBlogKey = null,
    isLoading = false,
}: SubscriptionsProps) => {
    const [expanded, setExpanded] = useState(false)

    const safeItems = useMemo<SubscriptionItem[]>(
        () => (Array.isArray(items) ? items : []),
        [items]
    )

    const hasMore = safeItems.length > maxVisible
    const visibleItems = useMemo(() => {
        if (!hasMore) return safeItems
        return expanded ? safeItems : safeItems.slice(0, maxVisible)
    }, [safeItems, expanded, hasMore, maxVisible])

    if (isLoading) {
        return (
            <div className={styles.wrap} aria-busy="true">
                <button
                    type="button"
                    className={`${styles.header} ${headerActive ? styles.active : ""}`}
                    onClick={onClickHeader}
                    disabled
                >
                    <span className={styles.title}>구독</span>
                    <ArrowForwardIosIcon sx={{ fontSize: 14, color: "#252525" }} />
                </button>

                <ul className={styles.list}>
                    {Array.from({ length: maxVisible }).map((_, i) => (
                        <li key={`s-${i}`}>
                            <div className={`${styles.item} ${styles.skeletonRow}`}>
                                <div className={`${styles.avatarWrap} ${styles.skeleton} ${styles.skeletonCircle}`}>
                                    <div className={styles.avatar} />
                                </div>
                                <div className={`${styles.skeleton} ${styles.skeletonLine}`} />
                            </div>
                        </li>
                    ))}
                </ul>
            </div>
        )
    }

    if (safeItems.length === 0) {
        return (
            <div className={styles.wrap}>
                <button
                    type="button"
                    className={`${styles.header} ${headerActive ? styles.active : ""}`}
                    onClick={onClickHeader}
                >
                    <span className={styles.title}>구독</span>
                    <ArrowForwardIosIcon sx={{ fontSize: 14, color: "#252525" }} />
                </button>

                <div className={styles.empty}>
                    구독중인 기술 블로그가 <br /> 없습니다
                </div>
            </div>
        )
    }

    return (
        <div className={styles.wrap}>
            <button
                type="button"
                className={`${styles.header} ${headerActive ? styles.active : ""}`}
                onClick={onClickHeader}
            >
                <span className={styles.title}>구독</span>
                <ArrowForwardIosIcon sx={{ fontSize: 14, color: "#252525" }} />
            </button>

            <ul className={styles.list}>
                {visibleItems.map((it) => {
                    const isActiveItem = !!activeBlogKey && activeBlogKey.toString() === it.id.toString()
                    return (
                        <li key={it.id ?? it.key}>
                            <button
                                type="button"
                                className={`${styles.item} ${isActiveItem ? styles.activeItem : ""}`}
                                onClick={() => onClickItem?.(it)}
                            >
                                <div className={styles.avatarWrap}>
                                    <img
                                        className={styles.avatar}
                                        src={it.icon}
                                        alt={it.title ?? "tech blog"}
                                        loading="lazy"
                                    />
                                </div>
                                <span className={styles.name} title={it.title}>
                                    {it.title}
                                </span>
                            </button>
                        </li>
                    )
                })}
            </ul>

            {hasMore && (
                <button
                    type="button"
                    className={styles.more}
                    onClick={() => setExpanded((v) => !v)}
                >
                    <ExpandMoreIcon
                        className={`${styles.chev} ${expanded ? styles.up : ""}`}
                        fontSize="small"
                    />
                    <span>{expanded ? "접기" : "더보기"}</span>
                </button>
            )}
        </div>
    )
}

export default Subscriptions
