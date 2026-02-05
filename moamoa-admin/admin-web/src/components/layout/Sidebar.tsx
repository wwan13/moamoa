import styles from './Sidebar.module.css'
import CategoryOutlinedIcon from '@mui/icons-material/CategoryOutlined'
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined'
import {useLocation, useNavigate} from "react-router-dom"
import * as React from "react"

type Menu = {
    title: string
    key: string
    navigateTo: string
    icon: (active: boolean) => React.ReactNode
}

const menus: Menu[] = [
    {
        title: "대시보드",
        key: "/dashboard",
        navigateTo: "/dashboard",
        icon: (active) => (
            <DashboardOutlinedIcon
                sx={{ fontSize: 20, color: active ? "#000000" : "#808080" }}
            />
        ),
    },
    {
        title: "미분류 게시글",
        key: "/uncategorized",
        navigateTo: "/uncategorized",
        icon: (active) => (
            <CategoryOutlinedIcon
                sx={{ fontSize: 20, color: active ? "#000000" : "#808080" }}
            />
        ),
    },
]

const Sidebar = () => {
    const { pathname } = useLocation()
    const navigate = useNavigate()

    return (
        <section className={styles.wrap}>
            <div className={styles.top}>
                <div className={styles.logoWrap}>
                    <img
                        alt="moamoa admin logo"
                        src="https://i.imgur.com/dWAf3KG.png"
                        className={styles.logo}
                    />
                </div>

                <div className={styles.menus}>
                    {menus.map((menu) => {
                        const active =
                            pathname === menu.key ||
                            pathname.startsWith(`${menu.key}/`)

                        return (
                            <div
                                key={menu.key}
                                className={`${styles.menu} ${active ? styles.active : ""}`}
                                onClick={() => navigate(menu.navigateTo)}
                            >
                                <div className={styles.menuIcon}>
                                    {menu.icon(active)}
                                </div>
                                <p>{menu.title}</p>
                            </div>
                        )
                    })}
                </div>
            </div>
        </section>
    )
}

export default Sidebar