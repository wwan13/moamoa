import AppRoutes from "../routes/AppRoutes.tsx"
import { useEffect, useRef, useState } from "react"
import GlobalAlertModal from "../components/alert/GlobalAlertModal"
import GlobalConfirmModal from "../components/confirm/GlobalConfirmModal"
import GlobalToast from "../components/toast/GlobalToast"
import {
  setOnGlobalAlert,
  setOnGlobalConfirm,
  setOnNotFound,
  setOnServerError,
  setOnToast,
  type Toast,
} from "../api/client"
import { useNavigate } from "react-router-dom"

type ConfirmState = {
  title?: string
  message: string
  confirmText: string
  cancelText: string
  onConfirm?: () => void
  onCancel?: () => void
}

type ToastItem = Toast & {
  id: number
}

function App() {
  const [alertOpen, setAlertOpen] = useState(false)
  const [alertTitle, setAlertTitle] = useState("오류")
  const [alertMessage, setAlertMessage] = useState<string>("")
  const [alertOnClose, setAlertOnClose] = useState<() => void>(
    () => () => setAlertOpen(false),
  )

  const [toasts, setToasts] = useState<ToastItem[]>([])
  const toastSequence = useRef(0)

  const [confirmOpen, setConfirmOpen] = useState(false)
  const [confirmState, setConfirmState] = useState<ConfirmState>({
    message: "",
    confirmText: "확인",
    cancelText: "취소",
    onConfirm: () => {},
    onCancel: () => {},
  })
  const navigate = useNavigate()

  useEffect(() => {
    setOnServerError(({ message }) => {
      setAlertTitle("오류")
      setAlertMessage(message)
      setAlertOnClose(() => () => setAlertOpen(false))
      setAlertOpen(true)
    })

    setOnGlobalAlert(({ title, message, onClose }) => {
      setAlertTitle(title ?? "오류")
      setAlertMessage(message ?? "")
      setAlertOnClose(() => () => {
        setAlertOpen(false)
        onClose?.()
      })
      setAlertOpen(true)
    })

    setOnToast((toast) => {
      toastSequence.current += 1
      setToasts((prev) => [
        ...prev,
        {
          ...toast,
          id: toastSequence.current,
        },
      ])
    })

    setOnGlobalConfirm(
      ({ title, message, confirmText, cancelText, onConfirm, onCancel }) => {
        setConfirmState({
          title,
          message: message ?? "계속 진행할까요?",
          confirmText: confirmText ?? "확인",
          cancelText: cancelText ?? "취소",
          onConfirm: onConfirm ?? (() => {}),
          onCancel: onCancel ?? (() => {}),
        })
        setConfirmOpen(true)
      },
    )

    setOnNotFound(() => {
      navigate("/404")
    })
  }, [navigate])

  return (
    <div>
      <GlobalAlertModal
        open={alertOpen}
        title={alertTitle}
        message={alertMessage}
        onClose={alertOnClose}
      />

      <GlobalConfirmModal
        open={confirmOpen}
        title={confirmState.title}
        message={confirmState.message}
        confirmText={confirmState.confirmText}
        cancelText={confirmState.cancelText}
        onConfirm={() => {
          setConfirmOpen(false)
          confirmState.onConfirm?.()
        }}
        onCancel={() => {
          setConfirmOpen(false)
          confirmState.onCancel?.()
        }}
      />

      <GlobalToast
        toasts={toasts}
        onClose={(id) => {
          setToasts((prev) => prev.filter((toast) => toast.id !== id))
        }}
      />

      <AppRoutes />
    </div>
  )
}

export default App
