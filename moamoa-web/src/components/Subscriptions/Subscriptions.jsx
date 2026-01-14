import { useEffect, useMemo, useState } from "react"
import styles from "./Subscriptions.module.css"
import ExpandMoreIcon from "@mui/icons-material/ExpandMore"
import ArrowForwardIosIcon from "@mui/icons-material/ArrowForwardIos"

const SKELETON_DELAY_MS = 300

export default function Subscriptions({
                                          items,
                                          maxVisible = 5,
                                          onClickItem,
                                          onClickHeader,
                                          headerActive = false,
                                          activeBlogKey = null,
                                          isLoading = false,
                                      }) {
    const [expanded, setExpanded] = useState(false)

    // ✅ items를 무조건 배열로 보정 (라우팅/캐시 타이밍으로 객체/undefined가 들어와도 안전)
    const safeItems = useMemo(() => (Array.isArray(items) ? items : []), [items])

    // ✅ 300ms 이상 로딩일 때만 스켈레톤 표시
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

    // ✅ 더보기 여부/보이는 목록 계산
    const hasMore = safeItems.length > maxVisible
    const visibleItems = useMemo(() => {
        if (!hasMore) return safeItems
        return expanded ? safeItems : safeItems.slice(0, maxVisible)
    }, [safeItems, expanded, hasMore, maxVisible])

    // ✅ 스켈레톤
    if (showSkeleton) {
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

    // ✅ 비어있음
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
                    const isActiveItem = !!activeBlogKey && activeBlogKey === it.key
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