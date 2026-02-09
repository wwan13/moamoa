import Header from "../components/header/Header"
import Footer from "../components/footer/Footer"
import { Outlet } from "react-router-dom"
import styles from "./DefaultLayout.module.css"

const DefaultLayout = () => {
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
export default DefaultLayout
