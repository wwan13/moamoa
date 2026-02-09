import styles from "./GlobalConfirmModal.module.css"
import Button from "../ui/Button"

type GlobalConfirmModalProps = {
    open: boolean
    title?: string
    message?: string
    confirmText?: string
    cancelText?: string
    onConfirm?: () => void
    onCancel?: () => void
}

const GlobalConfirmModal = ({
    open,
    title,
    message,
    confirmText = "확인",
    cancelText = "취소",
    onConfirm,
    onCancel,
}: GlobalConfirmModalProps) => {
    if (!open) return null

    return (
        <div className={styles.backdrop}>
            <div className={styles.panel} role="alertdialog" aria-modal="true">
                {title && <h3 className={styles.title}>{title}</h3>}
                <p className={styles.message}>{message}</p>

                <div className={styles.actions}>
                    <div className={styles.actionsRow}>
                        <Button variant="border" fullWidth={false} type="button" onClick={onCancel}>
                            {cancelText}
                        </Button>
                        <Button variant="primary" fullWidth={false} type="button" onClick={onConfirm}>
                            {confirmText}
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    )
}
export default GlobalConfirmModal
