import { useEffect, useState } from "react"
import styles from "./GlobalToast.module.css"
import type { Toast } from "../../api/client"

type GlobalToastProps = {
    toast: Toast | null
    onClose: () => void
}

const GlobalToast = ({
    toast,
    onClose,
}: GlobalToastProps) => {
    const [visible, setVisible] = useState(false)

    useEffect(() => {
        if (!toast) return

        setVisible(true)

        const hideTimer = setTimeout(() => {
            setVisible(false)
        }, toast.duration ?? 3000)

        const removeTimer = setTimeout(() => {
            onClose()
        }, (toast.duration ?? 3000) + 200)

        return () => {
            clearTimeout(hideTimer)
            clearTimeout(removeTimer)
        }
    }, [toast, onClose])

    if (!toast) return null

    return (
        <div
            className={`${styles.toast} ${
                visible ? styles.enter : styles.exit
            }`}
        >
            {toast.message}
        </div>
    )
}

export default GlobalToast
