import {useMemo, useState} from "react"
import styles from "./Subscriptions.module.css"
import ExpandMoreIcon from "@mui/icons-material/ExpandMore"

export default function Subscriptions({items = [], maxVisible = 5, onClickItem}) {
    const [expanded, setExpanded] = useState(false)

    const hasMore = items.length > maxVisible
    const visibleItems = useMemo(() => {
        if (!hasMore) return items
        return expanded ? items : items.slice(0, maxVisible)
    }, [items, expanded, hasMore, maxVisible])

    return (
        <div className={styles.wrap}>
            <div className={styles.header}>
                <span className={styles.title}>구독</span>
            </div>

            <ul className={styles.list}>
                {visibleItems.map((it) => (
                    <li key={it.id}>
                        <button
                            type="button"
                            className={styles.item}
                            onClick={() => onClickItem?.(it)}
                        >
                            <img className={styles.avatar} src="https://avatars.githubusercontent.com/u/25682207?s=200&v=4" alt={it.name}/>
                            <span className={styles.name} title={it.name}>
                                {it.name}
                            </span>
                        </button>
                    </li>
                ))}
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
                        color="#252525"
                    />
                    <span>
                        {expanded ? "접기" : "더보기"}
                    </span>
                </button>
            )}
        </div>
    )
}