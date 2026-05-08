import styles from "./SortTabs.module.css"

type SortTabItem<T extends string> = {
  key: T
  label: string
}

type SortTabsProps<T extends string> = {
  items: ReadonlyArray<SortTabItem<T>>
  value: T
  onChange: (nextValue: T) => void
}

const SortTabs = <T extends string>({
  items,
  value,
  onChange,
}: SortTabsProps<T>) => {
  return (
    <nav className={styles.wrap} aria-label="정렬">
      {items.map((item) => {
        const active = item.key === value

        return (
          <button
            key={item.key}
            type="button"
            className={`${styles.tab} ${active ? styles.active : ""}`}
            onClick={() => onChange(item.key)}
            aria-pressed={active}
          >
            {item.label}
          </button>
        )
      })}
    </nav>
  )
}

export default SortTabs
