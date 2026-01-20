import styles from "./GlobalConfirmModal.module.css"
import Button from "../ui/Button.jsx"

export default function GlobalConfirmModal({
                                               open,
                                               title = "확인",
                                               message,
                                               confirmText = "확인",
                                               cancelText = "취소",
                                               onConfirm,
                                               onCancel,
                                           }) {
    if (!open) return null

    return (
        <div className={styles.backdrop}>
            <div className={styles.panel} role="alertdialog" aria-modal="true">
                <h3 className={styles.title}>{title}</h3>
                <p className={styles.message}>{message}</p>

                <div className={styles.actions}>
                    <div className={styles.actionsRow}>
                        <Button className={styles.cancelButton} type="button" onClick={onCancel}>
                            {cancelText}
                        </Button>
                        <Button className={styles.confirmButton} type="button" onClick={onConfirm}>
                            {confirmText}
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    )
}