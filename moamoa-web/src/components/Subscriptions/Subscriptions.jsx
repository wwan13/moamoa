import { useMemo, useState } from "react"
import styles from "./Subscriptions.module.css"
import ExpandMoreIcon from "@mui/icons-material/ExpandMore"
import ArrowForwardIosIcon from "@mui/icons-material/ArrowForwardIos"

export default function Subscriptions({
                                          items = [],
                                          maxVisible = 5,
                                          onClickItem,
                                          onClickHeader,
                                          headerActive = false,
                                          activeBlogKey = null,
                                      }) {
    const [expanded, setExpanded] = useState(false)

    const hasMore = items.length > maxVisible
    const visibleItems = useMemo(() => {
        if (!hasMore) return items
        return expanded ? items : items.slice(0, maxVisible)
    }, [items, expanded, hasMore, maxVisible])

    if (items.length === 0) {
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
                        <li key={it.id}>
                            <button
                                type="button"
                                className={`${styles.item} ${isActiveItem ? styles.activeItem : ""}`}
                                onClick={() => onClickItem?.(it)}
                            >
                                <div className={styles.avatarWrap}>
                                    <img className={styles.avatar} src={it.icon} alt={it.title} />
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
                    <ExpandMoreIcon className={`${styles.chev} ${expanded ? styles.up : ""}`} fontSize="small" />
                    <span>{expanded ? "접기" : "더보기"}</span>
                </button>
            )}
        </div>
    )
}