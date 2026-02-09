import Header from "../components/Header/Header"
import Footer from "../components/Footer/Footer"
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
