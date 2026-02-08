import { useEffect, useMemo, useRef, useState } from "react"
import KeyboardArrowUpOutlinedIcon from "@mui/icons-material/KeyboardArrowUpOutlined"
import KeyboardArrowDownOutlinedIcon from "@mui/icons-material/KeyboardArrowDownOutlined"
import styles from "./Dropdown.module.css"

type DropdownOption = {
    value: string
    label: string
    disabled?: boolean
}

type DropdownProps = {
    options: DropdownOption[]
    value?: string
    defaultValue?: string
    placeholder?: string
    disabled?: boolean
    onChange?: (value: string, option: DropdownOption) => void
}

export function Dropdown({
    options,
    value,
    defaultValue,
    placeholder = "Select",
    disabled = false,
    onChange,
}: DropdownProps) {
    const isControlled = value !== undefined
    const [open, setOpen] = useState(false)
    const [internalValue, setInternalValue] = useState(defaultValue ?? "")
    const rootRef = useRef<HTMLDivElement | null>(null)

    const selectedValue = isControlled ? value : internalValue
    const selectedOption = useMemo(
        () => options.find((option) => option.value === selectedValue),
        [options, selectedValue]
    )

    useEffect(() => {
        if (!open) return

        const onPointerDown = (event: MouseEvent) => {
            if (!rootRef.current) return
            if (!rootRef.current.contains(event.target as Node)) {
                setOpen(false)
            }
        }

        const onKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                setOpen(false)
            }
        }

        document.addEventListener("mousedown", onPointerDown)
        document.addEventListener("keydown", onKeyDown)
        return () => {
            document.removeEventListener("mousedown", onPointerDown)
            document.removeEventListener("keydown", onKeyDown)
        }
    }, [open])

    const handleSelect = (option: DropdownOption) => {
        if (disabled || option.disabled) return
        if (!isControlled) setInternalValue(option.value)
        onChange?.(option.value, option)
        setOpen(false)
    }

    const handleButtonKeyDown = (event: React.KeyboardEvent) => {
        if (disabled) return
        if (event.key === "Enter" || event.key === " ") {
            event.preventDefault()
            setOpen((prev) => !prev)
        }
        if (event.key === "ArrowDown") {
            event.preventDefault()
            setOpen(true)
        }
    }

    return (
        <div ref={rootRef} className={`${styles.root}`}>
            <button
                type="button"
                disabled={disabled}
                className={`${styles.button} ${styles.small}`}
                aria-haspopup="listbox"
                aria-expanded={open}
                onClick={() => !disabled && setOpen((prev) => !prev)}
                onKeyDown={handleButtonKeyDown}
            >
                <span className={styles.label}>
                    {selectedOption?.label ?? placeholder}
                </span>
                <span className={styles.caret} aria-hidden="true">
                    {open ? (
                        <KeyboardArrowUpOutlinedIcon
                            sx={{ fontSize: 16, color: "#252525" }}
                        />
                    ) : (
                        <KeyboardArrowDownOutlinedIcon
                            sx={{ fontSize: 16, color: "#252525" }}
                        />
                    )}
                </span>
            </button>

            {open && (
                <ul
                    role="listbox"
                    className={`${styles.menu}`}
                >
                    {options.map((option) => {
                        const isSelected = option.value === selectedValue
                        return (
                            <li
                                key={option.value}
                                role="option"
                                aria-selected={isSelected}
                                onClick={() => handleSelect(option)}
                                className={`${styles.option} ${
                                    isSelected ? styles.selected : ""
                                } ${option.disabled ? styles.disabled : ""}`}
                            >
                                {option.label}
                            </li>
                        )
                    })}
                </ul>
            )}
        </div>
    )
}

export type { DropdownOption, DropdownProps }
