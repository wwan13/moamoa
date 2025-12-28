import styles from "./GlobalAlertModal.module.css"
import Button from "../ui/Button.jsx"

export default function GlobalAlertModal({ open, title = "오류", message, onClose }) {
    if (!open) return null

    return (
        <div className={styles.backdrop}>
            <div className={styles.panel} role="alertdialog" aria-modal="true">
                <h3 className={styles.title}>{title}</h3>
                <p className={styles.message}>{message}</p>

                <div className={styles.actions}>
                    <Button type="button" onClick={onClose}>
                        확인
                    </Button>
                </div>
            </div>
        </div>
    )
}