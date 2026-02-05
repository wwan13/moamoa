import AppRoutes from "../routes/AppRoutes.tsx"
import { useEffect, useState } from "react"
import GlobalAlertModal from "../components/alert/GlobalAlertModal"
import GlobalConfirmModal from "../components/confirm/GlobalConfirmModal"
import GlobalToast from "../components/toast/GlobalToast"
import {
    setOnGlobalAlert,
    setOnGlobalConfirm,
    setOnServerError,
    setOnToast,
    type Toast,
} from "../api/client"

type ConfirmState = {
    title: string
    message: string
    confirmText: string
    cancelText: string
    onConfirm?: () => void
    onCancel?: () => void
}

function App() {
    const [alertOpen, setAlertOpen] = useState(false)
    const [alertTitle, setAlertTitle] = useState("오류")
    const [alertMessage, setAlertMessage] = useState<string>("")
    const [alertOnClose, setAlertOnClose] = useState<() => void>(() => () => setAlertOpen(false))

    const [toast, setToast] = useState<Toast | null>(null)

    const [confirmOpen, setConfirmOpen] = useState(false)
    const [confirmState, setConfirmState] = useState<ConfirmState>({
        title: "확인",
        message: "",
        confirmText: "확인",
        cancelText: "취소",
        onConfirm: () => {},
        onCancel: () => {},
    })

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

        setOnToast(setToast)

        setOnGlobalConfirm(({ title, message, confirmText, cancelText, onConfirm, onCancel }) => {
            setConfirmState({
                title: title ?? "확인",
                message: message ?? "계속 진행할까요?",
                confirmText: confirmText ?? "확인",
                cancelText: cancelText ?? "취소",
                onConfirm,
                onCancel,
            })
            setConfirmOpen(true)
        })
    }, [])

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

            <GlobalToast toast={toast} onClose={() => setToast(null)} />

            <AppRoutes />
        </div>
    )
}

export default App
