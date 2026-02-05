import { useEffect } from "react"
import styles from "./GlobalAlertModal.module.css"
import Button from "../ui/Button"

type GlobalAlertModalProps = {
    open: boolean
    title?: string
    message?: string
    onClose: () => void
}

const GlobalAlertModal = ({
    open,
    title = "오류",
    message,
    onClose,
}: GlobalAlertModalProps) => {
    useEffect(() => {
        if (!open) return

        const onKeyDown = (e: KeyboardEvent) => {
            if (e.key === "Enter") {
                e.preventDefault()
                onClose()
            }
        }

        window.addEventListener("keydown", onKeyDown)
        return () => window.removeEventListener("keydown", onKeyDown)
    }, [open, onClose])

    if (!open) return null

    return (
        <div className={styles.backdrop}>
            <div className={styles.panel} role="alertdialog" aria-modal="true">
                <h3 className={styles.title}>{title}</h3>
                <p className={styles.message}>{message}</p>

                <div className={styles.actions}>
                    <Button type="button" onClick={onClose} autoFocus>
                        확인
                    </Button>
                </div>
            </div>
        </div>
    )
}

export default GlobalAlertModal
