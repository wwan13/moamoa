import { useEffect, useRef } from "react"
import styles from "./GlobalToast.module.css"
import type { Toast } from "../../api/client"

type ToastItem = Toast & {
  id: number
}

type GlobalToastProps = {
  toasts: ToastItem[]
  onClose: (id: number) => void
}

type ToastCardProps = {
  toast: ToastItem
  onClose: (id: number) => void
}

const ToastCard = ({ toast, onClose }: ToastCardProps) => {
  const onCloseRef = useRef(onClose)

  useEffect(() => {
    onCloseRef.current = onClose
  }, [onClose])

  useEffect(() => {
    const removeTimer = setTimeout(
      () => {
        onCloseRef.current(toast.id)
      },
      toast.duration ?? 3000,
    )

    return () => {
      clearTimeout(removeTimer)
    }
  }, [toast.id, toast.duration])

  return (
    <div className={`${styles.toast} ${styles.enter}`}>{toast.message}</div>
  )
}

const GlobalToast = ({ toasts, onClose }: GlobalToastProps) => {
  if (toasts.length === 0) return null

  return (
    <div className={styles.viewport}>
      {toasts
        .slice()
        .reverse()
        .map((toast) => (
          <ToastCard key={toast.id} toast={toast} onClose={onClose} />
        ))}
    </div>
  )
}

export default GlobalToast
