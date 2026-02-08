import { useState, type CSSProperties, type ReactNode } from "react"
import KeyboardArrowDownOutlinedIcon from "@mui/icons-material/KeyboardArrowDownOutlined"
import styles from "./ListItem.module.css"

type ListItemProps = {
    cells: ReactNode[]
    templateColumns?: string
    children?: ReactNode
    open?: boolean
    defaultOpen?: boolean
    onOpenChange?: (open: boolean) => void
    disabled?: boolean
}

export function ListItem({
    cells,
    templateColumns,
    children,
    open,
    defaultOpen = false,
    onOpenChange,
    disabled = false,
}: ListItemProps) {
    const isControlled = open !== undefined
    const [internalOpen, setInternalOpen] = useState(defaultOpen)
    const isOpen = isControlled ? open : internalOpen
    const expandable = Boolean(children)
    const gridTemplateColumns =
        templateColumns ?? `repeat(${cells.length}, minmax(0, 1fr))`
    const style = { gridTemplateColumns } as CSSProperties

    const handleToggle = () => {
        if (!expandable || disabled) return
        const next = !isOpen
        if (!isControlled) setInternalOpen(next)
        onOpenChange?.(next)
    }

    return (
        <div className={styles.root} role="row">
            {expandable ? (
                <div
                    className={styles.summaryButton}
                    role="button"
                    tabIndex={disabled ? -1 : 0}
                    aria-expanded={isOpen}
                    onClick={(event) => {
                        if (
                            (event.target as HTMLElement).closest(
                                '[data-no-row-toggle="true"]'
                            )
                        ) {
                            return
                        }
                        handleToggle()
                    }}
                    onKeyDown={(event) => {
                        if (disabled) return
                        if (event.target !== event.currentTarget) return
                        if (event.key === "Enter" || event.key === " ") {
                            event.preventDefault()
                            handleToggle()
                        }
                    }}
                >
                    <div className={styles.summaryGrid} style={style}>
                        {cells.map((cell, index) => (
                            <div key={index} className={styles.cell}>
                                {cell}
                            </div>
                        ))}
                        <span
                            className={`${styles.toggle} ${
                                isOpen ? styles.toggleOpen : ""
                            }`}
                            aria-hidden="true"
                        >
                            <KeyboardArrowDownOutlinedIcon
                                sx={{ fontSize: 18 }}
                            />
                        </span>
                    </div>
                </div>
            ) : (
                <div className={styles.summaryStatic}>
                    <div className={styles.summaryGrid} style={style}>
                        {cells.map((cell, index) => (
                            <div key={index} className={styles.cell}>
                                {cell}
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {expandable && isOpen && <div className={styles.details}>{children}</div>}
        </div>
    )
}

export type { ListItemProps }
