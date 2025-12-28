import { useRef } from "react"
import styles from "./ModalShell.module.css"
import useModalA11y from "../../hooks/useModalAccessibility.js"

export default function ModalShell({ open, title, onClose, children }) {
    const panelRef = useRef(null)
    useModalA11y({ open, onClose, panelRef })

    if (!open) return null

    return (
        <div
            className={styles.backdrop}
            role="dialog"
            aria-modal="true"
            aria-label={title}
            onMouseDown={(e) => {
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