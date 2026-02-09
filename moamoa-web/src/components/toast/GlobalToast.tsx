import { useEffect } from "react"
import styles from "./GlobalToast.module.css"

const GlobalToast = ({ toast, onClose }) => {
    useEffect(() => {
        if (!toast) return

        const removeTimer = setTimeout(() => {
            onClose()
        }, toast.duration ?? 3000)

        return () => {
            clearTimeout(removeTimer)
        }
    }, [toast, onClose])

    if (!toast) return null

    return (
        <div
            className={`${styles.toast} ${styles.enter}`}
        >
            {toast.message}
        </div>
    )
}
export default GlobalToast
