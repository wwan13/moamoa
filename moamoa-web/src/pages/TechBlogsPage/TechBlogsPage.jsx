import { useEffect, useMemo, useState } from "react"
import styles from "./TechBlogsPage.module.css"
import { useCountUp } from "../../hooks/useCountUp.js"
import { techBlogsApi } from "../../api/techblog.api.js"
import useAuth from "../../auth/AuthContext.jsx"
import { subscribingBlogsApi, subscriptionToggleApi } from "../../api/subscription.api.js"
import {showGlobalConfirm, showToast} from "../../api/client.js";

export default function TechBlogsPage() {
    const { isLoggedIn } = useAuth()

    const [blogCount, setBlogCount] = useState(1)
    const [blogs, setBlogs] = useState([])
    const [subscriptions, setSubscriptions] = useState([])
    const [search, setSearch] = useState("")

    const animated = useCountUp(blogCount, 900)

    useEffect(() => {
        const fetchBlogs = async () => {
            const res = await techBlogsApi()
            setBlogs(res)
            setBlogCount(res.length)
        }

        const fetchSubscriptions = async () => {
            if (!isLoggedIn) return
            const res = await subscribingBlogsApi()
            setSubscriptions(res)
        }

        fetchBlogs()
        fetchSubscriptions()
    }, [isLoggedIn])

    const subscribedBlogIdSet = useMemo(() => {
        return new Set(subscriptions.map((s) => s.id))
    }, [subscriptions])

    const filteredBlogs = useMemo(() => {
        const q = search.trim().toLowerCase()
        if (!q) return blogs

        return blogs.filter((b) => {
            const title = (b.title ?? "").toLowerCase()
            const key = (b.key ?? "").toLowerCase()
            return title.includes(q) || key.includes(q)
        })
    }, [blogs, search])

    const subscriptionToggle = async (techBlogId) => {
        const wasSubscribed = subscribedBlogIdSet.has(techBlogId)

        // ✅ 구독 해제는 "먼저" 컨펌
        if (wasSubscribed) {
            const ok = await showGlobalConfirm({
                title: "구독 해제",
                message: "이 기술 블로그 구독을 해제할까요?",
                confirmText: "해제",
                cancelText: "유지",
            })

            if (!ok) return
        }

        // ✅ optimistic update
        setSubscriptions((prev) => {
            if (wasSubscribed) return prev.filter((s) => s.id !== techBlogId)
            return [...prev, { id: techBlogId }]
        })

        try {
            await subscriptionToggleApi(techBlogId)

            // ✅ 성공 토스트
            showToast(wasSubscribed ? "구독을 해제했어요." : "구독했어요.")
        } catch (e) {
            // ✅ rollback
            setSubscriptions((prev) => {
                if (wasSubscribed) return [...prev, { id: techBlogId }]
                return prev.filter((s) => s.id !== techBlogId)
            })

            // 실패 메시지(원하면 alert로 바꿔도 됨)
            showToast("처리 중 오류가 발생했어요. 다시 시도해 주세요.")
        }
    }

    const stop = (e) => {
        e.preventDefault()
        e.stopPropagation()
    }

    return (
        <div className={styles.wrap}>
            <section className={styles.info}>
                <div className={styles.infoInner}>
                    <div className={styles.textSection}>
                        <h1 className={styles.title}>
                            <span className={styles.count}>{animated}</span>개의 기술 블로그를
                            <br />
                            모아보고 있어요
                        </h1>
                    </div>

                    <div className={styles.buttonSection}>
                        <p className={styles.ctaText}>찾으시는 기술 블로그가 없다면</p>
                        <div className={styles.ctaRow}>
                            <button className={styles.primaryButton}>요청하기</button>
                        </div>
                    </div>
                </div>
            </section>

            <section className={styles.listSection}>
                <div className={styles.listHeader}>
                    <div className={styles.controls}>
                        <input
                            className={styles.search}
                            placeholder="검색"
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                        />
                    </div>
                </div>

                {filteredBlogs.length === 0 ? (
                    <div className={styles.empty}>
                        <p>검색 결과가 없습니다</p>
                    </div>
                ) : (
                    <div className={styles.grid}>
                        {filteredBlogs.map((blog) => {
                            const isSubscribed = subscribedBlogIdSet.has(blog.id)

                            return (
                                <article key={blog.id} className={styles.card}>
                                    <div className={styles.logoWrap}>
                                        <img src={blog.icon} alt="thumbnail" className={styles.logo} />
                                    </div>
                                    <p className={styles.blogName}>{blog.title}</p>

                                    <div className={styles.subscription}>
                                        <p className={styles.subscriptionCount}>구독자 3명</p>

                                        {isLoggedIn && (
                                            <>
                                                <span>·</span>
                                                <button
                                                    className={isSubscribed ? styles.subscribing : styles.subButton}
                                                    onClick={(e) => {
                                                        stop(e)
                                                        subscriptionToggle(blog.id)
                                                    }}
                                                >
                                                    {isSubscribed ? "구독중" : "구독"}
                                                </button>
                                            </>
                                        )}
                                    </div>
                                </article>
                            )
                        })}
                    </div>
                )}
            </section>
        </div>
    )
}