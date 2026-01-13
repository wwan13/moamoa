import { useEffect, useMemo, useState } from "react"
import styles from "./TechBlogsPage.module.css"
import { useCountUp } from "../../hooks/useCountUp.js"
import { techBlogsApi } from "../../api/techblog.api.js"
import useAuth from "../../auth/AuthContext.jsx"
import { subscribingBlogsApi, subscriptionToggleApi } from "../../api/subscription.api.js"
import {showGlobalConfirm, showToast} from "../../api/client.js";
import {useNavigate} from "react-router-dom";
import NotificationsNoneOutlinedIcon from '@mui/icons-material/NotificationsNoneOutlined';

export default function TechBlogsPage() {
    const navigate = useNavigate()
    const { isLoggedIn, openLogin } = useAuth()

    const [blogCount, setBlogCount] = useState(0)
    const [blogs, setBlogs] = useState([])
    const [search, setSearch] = useState("")

    const animated = useCountUp(blogCount, 900)

    useEffect(() => {
        const fetchBlogs = async () => {
            const res = await techBlogsApi()
            console.log(res)
            setBlogs(res.techBlogs)
            setBlogCount(res.meta.totalCount)
        }

        fetchBlogs()
    }, [isLoggedIn])

    const filteredBlogs = useMemo(() => {
        const q = search.trim().toLowerCase()
        if (!q) return blogs

        return blogs.filter((b) => {
            const title = (b.title ?? "").toLowerCase()
            const key = (b.key ?? "").toLowerCase()
            return title.includes(q) || key.includes(q)
        })
    }, [blogs, search])

    const subscriptionToggle = async (blog) => {
        if (!isLoggedIn) {
            openLogin()
            return
        }

        const wasSubscribed = !!blog.subscribed
        const techBlogId = blog.id

        if (wasSubscribed) {
            const ok = await showGlobalConfirm({
                title: "구독 해제",
                message: "이 기술 블로그 구독을 해제할까요?",
                confirmText: "해제",
                cancelText: "유지",
            })
            if (!ok) return
        }

        // ✅ optimistic update: blog.subscribed + (있다면) 카운트도 같이
        setBlogs((prev) =>
            prev.map((b) =>
                b.id === techBlogId
                    ? {
                        ...b,
                        subscribed: !wasSubscribed,
                        // 선택: 서버가 subscriptionCount도 의미 있게 준다면 같이 토글
                        subscriptionCount: (b.subscriptionCount ?? 0) + (wasSubscribed ? -1 : 1),
                    }
                    : b
            )
        )

        try {
            await subscriptionToggleApi(techBlogId)
            showToast(wasSubscribed ? "구독을 해제했어요." : "구독했어요.")
        } catch (e) {
            // ✅ rollback
            setBlogs((prev) =>
                prev.map((b) =>
                    b.id === techBlogId
                        ? {
                            ...b,
                            subscribed: wasSubscribed,
                            subscriptionCount: (b.subscriptionCount ?? 0) + (wasSubscribed ? 1 : -1),
                        }
                        : b
                )
            )
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
                        <p>기술 블로그가 존재하지 않습니다.</p>
                    </div>
                ) : (
                    <div className={styles.grid}>
                        {filteredBlogs.map((blog) => {
                            return (
                                <article
                                    key={blog.id}
                                    className={styles.card}
                                    onClick={() => navigate(`/${blog.key}`)}
                                >
                                    <div className={styles.logoWrap}>
                                        <img src={blog.icon} alt="thumbnail" className={styles.logo} />
                                    </div>
                                    <p className={styles.blogName}>{blog.title}</p>

                                    <div className={styles.subscription}>
                                        <p className={styles.subscriptionCount}>구독자 {blog.subscriptionCount}명 · 게시글 {blog.postCount}개</p>

                                        {/*<span>·</span>*/}
                                        {/*<button*/}
                                        {/*    className={isSubscribed ? styles.subscribing : styles.subButton}*/}
                                        {/*    onClick={(e) => {*/}
                                        {/*        stop(e)*/}
                                        {/*        subscriptionToggle(blog.id)*/}
                                        {/*    }}*/}
                                        {/*>*/}
                                        {/*    {isSubscribed ? "구독중" : "구독"}*/}
                                        {/*</button>*/}

                                        {/*<span>·</span>*/}
                                        {/*<NotificationsNoneOutlinedIcon fontSize="10px"/>*/}
                                    </div>
                                    <div className={styles.subscription}>
                                        <button
                                            className={blog.subscribed ? styles.subscribing : styles.subButton}
                                            onClick={(e) => {
                                                stop(e)
                                                subscriptionToggle(blog)
                                            }}
                                        >
                                            {blog.subscribed ? "구독중" : "구독"}
                                        </button>

                                        <span>·</span>
                                        <NotificationsNoneOutlinedIcon fontSize="10px"/>
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