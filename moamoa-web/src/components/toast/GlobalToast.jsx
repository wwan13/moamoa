import { useEffect, useState } from "react"
import styles from "./GlobalToast.module.css"

export default function GlobalToast({ toast, onClose }) {
    const [visible, setVisible] = useState(false)

    useEffect(() => {
        if (!toast) return

        setVisible(true)

        const hideTimer = setTimeout(() => {
            setVisible(false) // 🔽 내려가는 애니메이션
        }, toast.duration ?? 3000)

        const removeTimer = setTimeout(() => {
            onClose()
        }, (toast.duration ?? 3000) + 200) // 애니메이션 시간만큼 대기

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