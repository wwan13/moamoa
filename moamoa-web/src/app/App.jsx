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
} from "../api/client.js"

import GlobalSpinner from "../components/GlobalSpinner/GlobalSpinner.jsx"
import GlobalAlertModal from "../components/alert/GlobalAlertModal.jsx"
import GlobalToast from "../components/toast/GlobalToast.jsx"

export default function App() {
    const [loading, setLoading] = useState(false)
    const [alertOpen, setAlertOpen] = useState(false)
    const [alertMessage, setAlertMessage] = useState("")
    const [toast, setToast] = useState(null)

    useEffect(() => {
        setOnLoadingChange(setLoading)

        setOnServerError(({ message }) => {
            setAlertMessage(message)
            setAlertOpen(true)
        })

        setOnGlobalAlert(({ message }) => {
            setAlertMessage(message)
            setAlertOpen(true)
        })

        setOnToast(setToast)
    }, [])

    return (
        <div className={styles.page}>
            {loading && <GlobalSpinner />}

            <GlobalAlertModal
                open={alertOpen}
                message={alertMessage}
                onClose={() => setAlertOpen(false)}
            />

            <GlobalToast toast={toast} onClose={() => setToast(null)} />

            <header className={styles.header}>
                <Header />
            </header>

            <main className={styles.main}>
                <AppRoutes />
            </main>

            <Footer />
        </div>
    )
}