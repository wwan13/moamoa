import {useEffect, useState} from "react"
import styles from "./App.module.css"
import LeftSidebar from "../components/LeftSideBar/LeftSidebar.jsx"
import PostList from "../components/PostList/PostList.jsx"
import Header from "../components/Header/Header.jsx";
import useAuth from "../auth/AuthContext.jsx";
import {setOnGlobalAlert, setOnLoadingChange, setOnServerError, setOnToast} from "../api/client.js";
import GlobalSpinner from "../components/GlobalSpinner/GlobalSpinner.jsx";
import GlobalAlertModal from "../components/alert/GlobalAlertModal.jsx";
import GlobalToast from "../components/toast/GlobalToast.jsx";

const MOCK_SUBS = [
    { id: "toss", name: "Toss Tech", iconUrl: "https://avatars.githubusercontent.com/u/25682207?s=200&v=4" },
    { id: "woowa", name: "우아한형제들", iconUrl: "https://avatars.githubusercontent.com/u/25682207?s=200&v=4" },
    { id: "woowa", name: "우아한형제들", iconUrl: "https://avatars.githubusercontent.com/u/25682207?s=200&v=4" },
    { id: "woowa", name: "우아한형제들", iconUrl: "https://avatars.githubusercontent.com/u/25682207?s=200&v=4" },
    { id: "woowa", name: "우아한형제들", iconUrl: "https://avatars.githubusercontent.com/u/25682207?s=200&v=4" },
    { id: "woowa", name: "우아한형제들", iconUrl: "https://avatars.githubusercontent.com/u/25682207?s=200&v=4" },
    { id: "woowa", name: "우아한형제들", iconUrl: "https://avatars.githubusercontent.com/u/25682207?s=200&v=4" },
    { id: "woowa", name: "우아한형제들", iconUrl: "https://avatars.githubusercontent.com/u/25682207?s=200&v=4" },

]

const MOCK_POSTS = [
    {
        id: "1",
        title: "예시 글 제목",
        summary: "예시 요약 텍스트…",
        category: "개발",
        publishedAt: "2025-12-27",
    },
    {
        id: "2",
        title: "또 다른 글",
        summary: "카테고리/태그로 필터링 되는 리스트",
        category: "프로덕트",
        publishedAt: "2025-12-26",
    },
    {
        id: "2",
        title: "또 다른 글",
        summary: "카테고리/태그로 필터링 되는 리스트",
        category: "프로덕트",
        publishedAt: "2025-12-26",
    },
    {
        id: "2",
        title: "또 다른 글",
        summary: "카테고리/태그로 필터링 되는 리스트",
        category: "프로덕트",
        publishedAt: "2025-12-26",
    },
    {
        id: "2",
        title: "또 다른 글",
        summary: "카테고리/태그로 필터링 되는 리스트",
        category: "프로덕트",
        publishedAt: "2025-12-26",
    },
    {
        id: "2",
        title: "또 다른 글",
        summary: "카테고리/태그로 필터링 되는 리스트",
        category: "프로덕트",
        publishedAt: "2025-12-26",
    },
    {
        id: "2",
        title: "또 다른 글",
        summary: "카테고리/태그로 필터링 되는 리스트",
        category: "프로덕트",
        publishedAt: "2025-12-26",
    },
    {
        id: "2",
        title: "또 다른 글",
        summary: "카테고리/태그로 필터링 되는 리스트",
        category: "프로덕트",
        publishedAt: "2025-12-26",
    },
    {
        id: "2",
        title: "또 다른 글",
        summary: "카테고리/태그로 필터링 되는 리스트",
        category: "프로덕트",
        publishedAt: "2025-12-26",
    },
    {
        id: "2",
        title: "또 다른 글",
        summary: "카테고리/태그로 필터링 되는 리스트",
        category: "프로덕트",
        publishedAt: "2025-12-26",
    },

]

export default function App() {
    const [loading, setLoading] = useState(false)

    const [alertOpen, setAlertOpen] = useState(false)
    const [alertMessage, setAlertMessage] = useState("")

    const [toast, setToast] = useState(null)

    const { isLoggedIn } = useAuth()

    const [posts] = useState(MOCK_POSTS)
    const [subs] = useState(MOCK_SUBS)

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

            <section className={styles.banner}>
                <img
                    src="https://i.imgur.com/rfwgfw2.png"
                    alt="banner"
                    className={styles.bannerImage}
                />
            </section>

            {isLoggedIn ? (
                <main className={styles.layout}>
                    <aside className={styles.left}>
                        <LeftSidebar subscriptions={subs} />
                    </aside>

                    <section className={styles.content}>
                        <PostList posts={posts} />
                    </section>
                </main>
            ) : (
                <section className={styles.content}>
                    <PostList posts={posts} />
                </section>
            )}
        </div>
    )
}
