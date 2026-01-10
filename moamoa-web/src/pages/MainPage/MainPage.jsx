import { useEffect, useState } from "react"
import styles from "./MainPage.module.css"

import LeftSidebar from "../../components/LeftSideBar/LeftSidebar.jsx"
import PostList from "../../components/PostList/PostList.jsx"
import useAuth from "../../auth/AuthContext.jsx"
import { subscribingBlogsApi } from "../../api/subscription.api.js"
import {postsApi, postsByBookmarkApi, postsBySubscriptionApi, postsByTechBlogKeyApi} from "../../api/post.api.js"
import { useSearchParams } from "react-router-dom"

const TYPES = {
    ALL: "all",
    SUBSCRIBED: "subscribed",
    BOOKMARKED: "bookmarked",
}

export default function MainPage() {
    const { isLoggedIn } = useAuth()
    const [searchParams, setSearchParams] = useSearchParams()

    // ✅ URL이 단일 상태
    const type = searchParams.get("type") ?? TYPES.ALL
    const blogKey = searchParams.get("techBlog") ?? null
    const page = Number(searchParams.get("page") ?? 1)

    const [totalPages, setTotalPages] = useState(1)
    const [posts, setPosts] = useState([])
    const [subs, setSubs] = useState([])

    const [emptyMessage, setEmptyMessage] = useState("게시글이 존재하지 않습니다.")

    // ✅ 공통: subscriptions
    useEffect(() => {
        if (!isLoggedIn) return
        const fetchSubscriptions = async () => {
            try {
                const subsRes = await subscribingBlogsApi()
                setSubs(subsRes)
            } catch (e) {
            }
        }
        fetchSubscriptions()
    }, [isLoggedIn])

    // ✅ posts는 type/blogKey/page에 따라 갱신
    useEffect(() => {
        const fetchAllPosts = async () => {
            const postsRes = await postsApi({page})
            setTotalPages(postsRes.meta.totalPages)
            setPosts(postsRes.posts)
            window.scrollTo({ top: 0, behavior: "smooth" })
        }

        const fetchSubscribingPosts = async () => {
            const postsRes = await postsBySubscriptionApi({page})
            setTotalPages(postsRes.meta.totalPages)
            setPosts(postsRes.posts)
            window.scrollTo({ top: 0, behavior: "smooth" })
        }

        const fetchBookmarkedPosts = async () => {
            const postsRes = await postsByBookmarkApi({page})
            setTotalPages(postsRes.meta.totalPages)
            setPosts(postsRes.posts)
            window.scrollTo({ top: 0, behavior: "smooth" })
        }

        const fetchTechBlogPosts = async () => {
            const postsRes = await postsByTechBlogKeyApi({
                page: page,
                techBlogKey: blogKey
            })
            setTotalPages(postsRes.meta.totalPages)
            setPosts(postsRes.posts)
            window.scrollTo({ top: 0, behavior: "smooth" })
        }

        if (type === TYPES.ALL) {
            fetchAllPosts()
        } else if (type === TYPES.SUBSCRIBED) {
            if (blogKey === null) {
                if (subs.length === 0) {
                    setEmptyMessage("구독중인 블로그가 없습니다.")
                } else {
                    setEmptyMessage("구독중인 블로그에 게시글이 존재하지 않습니다.")
                }
                fetchSubscribingPosts()
            } else {
                setEmptyMessage("기술 블로그에 게시글이 존재하지 않습니다.")
                fetchTechBlogPosts()
            }
        } else if (type === TYPES.BOOKMARKED) {
            setEmptyMessage("북마크한 게시글이 없습니다.")
            fetchBookmarkedPosts()
        }

        // fetchAllPosts()
    }, [page, type, blogKey, isLoggedIn])

    // ✅ 핸들러들: URL 업데이트만 함
    const onChangePage = (nextPage) => {
        setSearchParams((prev) => {
            const p = new URLSearchParams(prev)
            p.set("page", String(nextPage))
            return p
        })
    }

    const onSelectType = (nextType) => {
        setSearchParams((prev) => {
            const p = new URLSearchParams(prev)

            if (nextType === TYPES.ALL) p.delete("type")
            else p.set("type", nextType)

            p.delete("techBlog")
            p.delete("page")
            return p
        })
    }

    const onSelectBlog = (nextBlogKey) => {
        setSearchParams((prev) => {
            const p = new URLSearchParams(prev)
            p.set("type", TYPES.SUBSCRIBED)
            p.set("techBlog", nextBlogKey)
            p.delete("page")
            return p
        })
    }

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
                        <LeftSidebar
                            subscriptions={subs}
                            type={type}
                            blogKey={blogKey}
                            onSelectType={onSelectType}
                            onSelectBlog={onSelectBlog}
                        />
                    </aside>

                    <section className={styles.content}>
                        <PostList
                            posts={posts}
                            page={page}
                            totalPages={totalPages}
                            onChangePage={onChangePage}
                            type={type}
                            emptyMessage={emptyMessage}
                        />
                    </section>
                </div>
            ) : (
                <section className={styles.content}>
                    <PostList
                        posts={posts}
                        page={page}
                        totalPages={totalPages}
                        onChangePage={onChangePage}
                        isBlogDetail={false}
                        emptyMessage={emptyMessage}
                    />
                </section>
            )}
        </>
    )
}