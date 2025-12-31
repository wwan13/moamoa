import { useEffect, useState } from "react"
import styles from "./App.module.css"
import Header from "../components/Header/Header.jsx"
import Footer from "../components/Footer/Footer.jsx"
import AppRoutes from "../routes/AppRoutes.jsx"

import {
    setOnGlobalAlert,
    setOnLoadingChange,
    setOnServerError,
    setOnToast,
    setOnGlobalConfirm,
} from "../api/client.js"

import GlobalSpinner from "../components/GlobalSpinner/GlobalSpinner.jsx"
import GlobalAlertModal from "../components/alert/GlobalAlertModal.jsx"
import GlobalToast from "../components/toast/GlobalToast.jsx"
import GlobalConfirmModal from "../components/confirm/GlobalConfirmModal.jsx"

export default function App() {
    const [loading, setLoading] = useState(false)

    // ✅ alert 상태 (Promise resolve용 onClose 포함)
    const [alertOpen, setAlertOpen] = useState(false)
    const [alertTitle, setAlertTitle] = useState("오류")
    const [alertMessage, setAlertMessage] = useState("")
    const [alertOnClose, setAlertOnClose] = useState(() => () => setAlertOpen(false))

    const [toast, setToast] = useState(null)

    // ✅ confirm 상태
    const [confirmOpen, setConfirmOpen] = useState(false)
    const [confirmState, setConfirmState] = useState({
        title: "확인",
        message: "",
        confirmText: "확인",
        cancelText: "취소",
        onConfirm: () => {},
        onCancel: () => {},
    })

    useEffect(() => {
        setOnLoadingChange(setLoading)

        // 서버 에러(기존 방식 유지: 단순 alert)
        setOnServerError(({ message }) => {
            setAlertTitle("오류")
            setAlertMessage(message)
            setAlertOnClose(() => () => setAlertOpen(false))
            setAlertOpen(true)
        })

        // ✅ Promise 기반 GlobalAlert 연결
        setOnGlobalAlert(({ title, message, onClose }) => {
            setAlertTitle(title ?? "오류")
            setAlertMessage(message)

            setAlertOnClose(() => () => {
                setAlertOpen(false)
                onClose?.() // ✅ showGlobalAlert() resolve
            })

            setAlertOpen(true)
        })

        setOnToast(setToast)

        // ✅ Promise 기반 GlobalConfirm 연결
        setOnGlobalConfirm(({ title, message, confirmText, cancelText, onConfirm, onCancel }) => {
            setConfirmState({
                title,
                message,
                confirmText,
                cancelText,
                onConfirm,
                onCancel,
            })
            setConfirmOpen(true)
        })
    }, [])

    return (
        <div className={styles.page}>
            {loading && <GlobalSpinner />}

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

            <header>
                <Header />
            </header>

            <main className={styles.main}>
                <AppRoutes />
            </main>

            <Footer />
        </div>
    )
}