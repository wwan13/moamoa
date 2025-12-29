import { useEffect, useState } from "react"
import styles from "./MainPage.module.css"

import LeftSidebar from "../../components/LeftSideBar/LeftSidebar.jsx"
import PostList from "../../components/PostList/PostList.jsx"
import useAuth from "../../auth/AuthContext.jsx"
import { subscribingBlogsApi } from "../../api/subscription.api.js"
import {usePagingQuery} from "../../hooks/usePagingQuery.js";
import {postsApi} from "../../api/post.api.js";

const MOCK_POSTS = [
    { id: "1", title: "예시 글 제목", summary: "예시 요약 텍스트…", category: "개발", publishedAt: "2025-12-27" },
    { id: "2", title: "또 다른 글", summary: "카테고리/태그로 필터링 되는 리스트", category: "프로덕트", publishedAt: "2025-12-26" },
]

export default function MainPage() {
    const { isLoggedIn } = useAuth()
    const { page, size, setPage } = usePagingQuery()

    const [totalPages, setTotalPages] = useState(1)
    const [posts, setPosts] = useState(MOCK_POSTS)
    const [subs, setSubs] = useState([])

    useEffect(() => {
        const fetchSubscriptions = async () => {
            const subsRes = await subscribingBlogsApi()
            setSubs(subsRes)
        }
        fetchSubscriptions()

        const fetchPosts = async () => {
            const postsRes = await postsApi(page)
            setTotalPages(postsRes.meta.totalPages)
            setPosts(postsRes.posts)
            window.scrollTo({ top: 0, behavior: "smooth" })
        }
        fetchPosts()
    }, [page, isLoggedIn])

    return (
        <>
            <section className={styles.banner}>
                <img
                    src="https://i.imgur.com/rfwgfw2.png"
                    alt="banner"
                    className={styles.bannerImage}
                />
            </section>

            {isLoggedIn ? (
                <div className={styles.layout}>
                    <aside className={styles.left}>
                        <LeftSidebar subscriptions={subs} />
                    </aside>

                    <section className={styles.content}>
                        <PostList
                            posts={posts}
                            page={page}
                            totalPages={totalPages}
                            onChangePage={setPage}
                        />
                    </section>
                </div>
            ) : (
                <section className={styles.content}>
                    <PostList
                        posts={posts}
                        page={page}
                        totalPages={totalPages}
                        onChangePage={setPage}   // ✅ URL 업데이트
                    />
                </section>
            )}
        </>
    )
}