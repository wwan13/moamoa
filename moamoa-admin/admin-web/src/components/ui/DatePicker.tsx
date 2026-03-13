import type { CSSProperties } from "react"
import styles from "./DatePicker.module.css"

type DatePickerProps = {
    value: string
    onChange: (value: string) => void
    min?: string
    max?: string
    disabled?: boolean
    width?: number | string
}

export const DatePicker = ({
    value,
    onChange,
    min,
    max,
    disabled = false,
    width,
}: DatePickerProps) => {
    const rootStyle: CSSProperties | undefined =
        width === undefined
            ? undefined
            : {
                  width: typeof width === "number" ? `${width}px` : width,
                  minWidth: typeof width === "number" ? `${width}px` : width,
              }

    return (
        <input
            type="datetime-local"
            value={value}
            min={min}
            max={max}
            disabled={disabled}
            className={styles.input}
            style={rootStyle}
            onChange={(event) => onChange(event.target.value)}
        />
    )
}

export type { DatePickerProps }
