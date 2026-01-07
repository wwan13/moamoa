import styles from "./CategoryTabs.module.css"

export default function CategoryTabs({items, id, onChange, isBlogDetail}) {
    return (
        <div className={styles.wrap}>
            <nav className={styles.tab} aria-label="카테고리">
                <div>
                    {items.map((it) => {
                        const active = it.id === id
                        return (
                            <button
                                key={it.key}
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
            </nav>
        </div>
    )
}