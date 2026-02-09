import AppRoutes from "../routes/AppRoutes"

import {
    setOnGlobalAlert,
    setOnServerError,
    setOnToast,
    setOnGlobalConfirm,
    setOnOpenSearch,
    setOnCloseSearch,
} from "../api/client"

import GlobalAlertModal from "../components/alert/GlobalAlertModal"
import GlobalToast from "../components/toast/GlobalToast"
import GlobalConfirmModal from "../components/confirm/GlobalConfirmModal"
import useAuth from "../auth/useAuth"
import LoginModal from "../components/LoginModal/LoginModal"
import Search from "../components/Search/Search" // 경로 맞춰

import { useEffect, useState } from "react"
import type { GlobalConfirmParams, Toast } from "../api/client"

const App = () => {
    // ✅ alert 상태
    const [alertOpen, setAlertOpen] = useState(false)
    const [alertTitle, setAlertTitle] = useState("오류")
    const [alertMessage, setAlertMessage] = useState("")
    const [alertOnClose, setAlertOnClose] = useState(() => () => setAlertOpen(false))

    const [toast, setToast] = useState<Toast | null>(null)

    // ✅ confirm 상태
    const [confirmOpen, setConfirmOpen] = useState(false)
    const [confirmState, setConfirmState] = useState<Required<Pick<GlobalConfirmParams, "title" | "message" | "confirmText" | "cancelText">> & Pick<GlobalConfirmParams, "onConfirm" | "onCancel">>({
        title: "확인",
        message: "",
        confirmText: "확인",
        cancelText: "취소",
        onConfirm: () => {},
        onCancel: () => {},
    })

    // ✅ search 상태 (추가)
    const [searchOpen, setSearchOpen] = useState(false)

    const { authModal, openSignup, closeAuthModal } = useAuth()

    useEffect(() => {
        setOnServerError(({ message }) => {
            setAlertTitle("오류")
            setAlertMessage(message)
            setAlertOnClose(() => () => setAlertOpen(false))
            setAlertOpen(true)
        })

        setOnGlobalAlert(({ title, message, onClose }) => {
            setAlertTitle(title ?? "오류")
            setAlertMessage(message)
            setAlertOnClose(() => () => {
                setAlertOpen(false)
                onClose?.()
            })
            setAlertOpen(true)
        })

        setOnToast(setToast)

        setOnGlobalConfirm(({ title, message, confirmText, cancelText, onConfirm, onCancel }) => {
            setConfirmState({ title, message, confirmText, cancelText, onConfirm, onCancel })
            setConfirmOpen(true)
        })

        // ✅ Search 전역 핸들러 등록 (추가)
        setOnOpenSearch(() => setSearchOpen(true))
        setOnCloseSearch(() => setSearchOpen(false))
    }, [])

    useEffect(() => {
        document.body.classList.toggle("modal-open", searchOpen)
        return () => {
            document.body.classList.remove("modal-open")
        }
    }, [searchOpen])

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

            <LoginModal
                open={authModal === "login"}
                onClose={closeAuthModal}
                onClickSignup={openSignup}
                onClickPasswordFind={() => {}}
            />

            {/* ✅ Search 모달 렌더링 (추가) */}
            <Search open={searchOpen} onClose={() => setSearchOpen(false)} />

            <AppRoutes />
        </div>
    )
}

export default App
