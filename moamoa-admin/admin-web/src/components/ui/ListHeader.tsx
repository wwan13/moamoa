import type { CSSProperties, ReactNode } from "react"
import styles from "./ListHeader.module.css"

type HeaderAlign = "left" | "center" | "right"

type ListHeaderColumn = {
    key: string
    label: ReactNode
    align?: HeaderAlign
}

type ListHeaderProps = {
    columns: ListHeaderColumn[]
    templateColumns?: string
}

export function ListHeader({ columns, templateColumns }: ListHeaderProps) {
    const gridTemplateColumns =
        templateColumns ?? `repeat(${columns.length}, minmax(0, 1fr))`
    const style = { gridTemplateColumns } as CSSProperties

    return (
        <div className={styles.root} role="row" style={style}>
            {columns.map((column) => (
                <div
                    key={column.key}
                    className={`${styles.cell} ${
                        column.align === "center"
                            ? styles.center
                            : column.align === "right"
                              ? styles.right
                              : styles.left
                    }`}
                    role="columnheader"
                >
                    {column.label}
                </div>
            ))}
        </div>
    )
}

export type { ListHeaderProps, ListHeaderColumn, HeaderAlign }

