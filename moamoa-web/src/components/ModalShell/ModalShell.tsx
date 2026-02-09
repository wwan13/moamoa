import { useRef, type MouseEvent, type ReactNode } from "react"
import styles from "./ModalShell.module.css"
import useModalA11y from "../../hooks/useModalAccessibility"

type ModalShellProps = {
    open: boolean
    title: string
    onClose: () => void
    children: ReactNode
}

const ModalShell = ({ open, title, onClose, children }: ModalShellProps) => {
    const panelRef = useRef<HTMLDivElement | null>(null)
    useModalA11y({ open, onClose, panelRef })

    if (!open) return null

    return (
        <div
            className={styles.backdrop}
            role="dialog"
            aria-modal="true"
            aria-label={title}
            onMouseDown={(e: MouseEvent<HTMLDivElement>) => {
                if (e.target === e.currentTarget) onClose()
            }}
        >
            <div className={styles.panel} ref={panelRef}>
                <div className={styles.header}>
                    <h2 className={styles.title}>{title}</h2>
                    <button type="button" className={styles.close} onClick={onClose} aria-label="닫기">
                        ×
                    </button>
                </div>

                <div className={styles.body}>{children}</div>
            </div>
        </div>
    )
}
export default ModalShell
