import styles from "./CategoryTabs.module.css"

export default function CategoryTabs({items, value, onChange}) {
    return (
        <div className={styles.wrap}>
            <nav className={styles.tab} aria-label="카테고리">
                <div>
                    {items.map((it) => {
                        const active = it.value === value
                        return (
                            <button
                                key={it.value}
                                type="button"
                                className={`${styles.tabItem} ${active ? styles.active : ""}`}
                                onClick={() => onChange(it.value)}
                                aria-current={active ? "page" : undefined}
                            >
                                {it.label}
                            </button>
                        )
                    })}
                </div>
                <button className={styles.moreButton}>
                    전체 기술 블로그 보기 >
                </button>
            </nav>
        </div>
    )
}