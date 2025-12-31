import { useEffect, useState } from "react"
import styles from "./MainPage.module.css"

import LeftSidebar from "../../components/LeftSideBar/LeftSidebar.jsx"
import PostList from "../../components/PostList/PostList.jsx"
import useAuth from "../../auth/AuthContext.jsx"
import { subscribingBlogsApi } from "../../api/subscription.api.js"
import {usePagingQuery} from "../../hooks/usePagingQuery.js";
import {postsApi} from "../../api/post.api.js";

export default function MainPage() {
    const { isLoggedIn } = useAuth()
    const { page, size, setPage } = usePagingQuery()

    const [totalPages, setTotalPages] = useState(1)
    const [posts, setPosts] = useState([])
    const [subs, setSubs] = useState([])

    useEffect(() => {
        const fetchSubscriptions = async () => {
            const subsRes = await subscribingBlogsApi()
            setSubs(subsRes)
        }
        fetchSubscriptions()

        const fetchPosts = async () => {
            const postsRes = await postsApi({ page: page })
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
                            isBlogDetail={false}
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