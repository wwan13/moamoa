import {Outlet, useNavigate} from "react-router-dom"
import styles from "./AppLayout.module.css"
import Sidebar from "./Sidebar.tsx";
import useAuth from "../../auth/AuthContext.tsx";
import {useEffect} from "react";

const AppLayout = () => {
    const { isLoggedIn } = useAuth()
    const navigate = useNavigate()

    useEffect(() => {
        if (!isLoggedIn) {
            navigate("/login")
        }
    }, [isLoggedIn]);

    return (
        <div className={styles.shell}>
            <Sidebar />
            <main className={styles.main}>
                {/*<Header />*/}
                <div className={styles.content}>
                    <Outlet />
                </div>
            </main>
        </div>
    )
}

export default AppLayout