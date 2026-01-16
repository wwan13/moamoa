import Header from "../components/Header/Header.jsx"
import Footer from "../components/Footer/Footer.jsx"
import { Outlet } from "react-router-dom"
import styles from "./DefaultLayout.module.css"

export default function DefaultLayout() {
    return (
        <div className={styles.page}>
            <Header />
            <main className={styles.main}>
                <Outlet />
            </main>
            <Footer />
        </div>
    )
}