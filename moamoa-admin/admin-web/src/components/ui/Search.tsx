import { useState } from "react"
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined"
import styles from "./Search.module.css"

type SearchProps = {
    value?: string
    defaultValue?: string
    placeholder?: string
    disabled?: boolean
    onChange?: (value: string) => void
    onSearch?: (value: string) => void
}

export function Search({
    value,
    defaultValue,
    placeholder = "검색어를 입력하세요",
    disabled = false,
    onChange,
    onSearch,
}: SearchProps) {
    const isControlled = value !== undefined
    const [internalValue, setInternalValue] = useState(defaultValue ?? "")
    const currentValue = isControlled ? value : internalValue

    const handleChange = (nextValue: string) => {
        if (!isControlled) setInternalValue(nextValue)
        onChange?.(nextValue)
    }

    const handleSearch = () => {
        if (disabled) return
        onSearch?.(currentValue)
    }

    return (
        <div className={styles.root}>
            <input
                type="text"
                className={styles.input}
                value={currentValue}
                onChange={(event) => handleChange(event.target.value)}
                onKeyDown={(event) => {
                    if (event.key === "Enter") {
                        event.preventDefault()
                        handleSearch()
                    }
                }}
                placeholder={placeholder}
                disabled={disabled}
            />
            <button
                type="button"
                className={styles.button}
                onClick={handleSearch}
                disabled={disabled}
                aria-label="검색"
            >
                <SearchOutlinedIcon sx={{ fontSize: 18 }} />
            </button>
        </div>
    )
}

export type { SearchProps }

