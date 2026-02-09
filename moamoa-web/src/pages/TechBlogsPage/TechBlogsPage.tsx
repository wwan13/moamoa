import { useMemo, useState, type MouseEvent } from "react"
import styles from "./TechBlogsPage.module.css"
import { useCountUp } from "../../hooks/useCountUp"
import useAuth from "../../auth/useAuth"
import { showGlobalConfirm, showToast } from "../../api/client"
import { useNavigate } from "react-router-dom"
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined"
import { useQueryClient } from "@tanstack/react-query"
import type { TechBlogSummary, TechBlogList } from "../../api/techBlog.api"

import { useTechBlogsQuery } from "../../queries/techBlog.queries"
import { useSubscriptionToggleMutation } from "../../queries/techBlogSubscription.queries"

const SKELETON_COUNT = 12

const TechBlogsPage = () => {
    const navigate = useNavigate()
    const { isLoggedIn, openLogin } = useAuth()
    const qc = useQueryClient()

    const [search, setSearch] = useState("")

    // ✅ tech blogs list (query)
    const techBlogsQuery = useTechBlogsQuery()

    const rawBlogs = useMemo(() => techBlogsQuery.data?.techBlogs ?? [], [techBlogsQuery.data])
    const totalCount = techBlogsQuery.data?.meta?.totalCount ?? rawBlogs.length
    const animated = useCountUp(totalCount, 900)

    const filteredBlogs = useMemo(() => {
        const q = search.trim().toLowerCase()
        if (!q) return rawBlogs

        return rawBlogs.filter((b) => {
            const title = (b.title ?? "").toLowerCase()
            const key = (b.key ?? "").toLowerCase()
            return title.includes(q) || key.includes(q)
        })
    }, [rawBlogs, search])

    // ✅ subscribe toggle (mutation)
    const subToggle = useSubscriptionToggleMutation()

    // ✅ optimistic 업데이트를 위해 techBlogs 캐시 수정
    const patchTechBlog = (techBlogId: number, patcher: (blog: TechBlogSummary) => TechBlogSummary) => {
        qc.setQueryData(["techBlogs"], (old: unknown) => {
            const cache = old as TechBlogList | undefined
            if (!cache?.techBlogs) return old
            return {
                ...cache,
                techBlogs: cache.techBlogs.map((b) => (b.id === techBlogId ? patcher(b) : b)),
            }
        })
    }

    const subscriptionToggle = async (blog: TechBlogSummary) => {
        if (!isLoggedIn) {
            const ok = await showGlobalConfirm({
                title : "로그인",
                message : "로그인이 필요한 기능입니다. 로그인 하시겠습니까?",
                confirmText : "로그인"
            })
            if (!ok) {
                return
            }
            openLogin()
            return
        }
        if (subToggle.isPending) return

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

        // optimistic
        patchTechBlog(techBlogId, (b) => ({
            ...b,
            subscribed: !wasSubscribed,
            subscriptionCount: (b.subscriptionCount ?? 0) + (wasSubscribed ? -1 : 1),
        }))

        try {
            await subToggle.mutateAsync({ techBlogId })
            showToast(wasSubscribed ? "구독을 해제했어요." : "구독했어요.")
        } catch {
            // rollback
            patchTechBlog(techBlogId, (b) => ({
                ...b,
                subscribed: wasSubscribed,
                subscriptionCount: (b.subscriptionCount ?? 0) + (wasSubscribed ? 1 : -1),
            }))
            showToast("처리 중 오류가 발생했어요. 다시 시도해 주세요.")
        }
    }

    const stop = (e: MouseEvent<HTMLElement>) => {
        e.preventDefault()
        e.stopPropagation()
    }

    const handleSubmissionButton = async () => {
        if (!isLoggedIn) {
            const ok = await showGlobalConfirm({
                title : "로그인",
                message : "로그인이 필요한 기능입니다. 로그인 하시겠습니까?",
                confirmText : "로그인"
            })
            if (!ok) {
                return
            }
            openLogin()
            return
        }

        navigate("/submission")
    }

    return (
        <div className={styles.wrap}>
            <section className={styles.info}>
                <div className={styles.infoInner}>
                    <div className={styles.textSection}>
                        <h1 className={styles.title}>
                            <span className={styles.count}>{animated}</span>
                            개의 기술 블로그를
                            <br />
                            모아보고 있어요
                        </h1>
                    </div>

                    <div className={styles.buttonSection}>
                        <p className={styles.ctaText}>찾으시는 기술 블로그가 없다면</p>
                        <div className={styles.ctaRow}>
                            <button
                                className={styles.primaryButton}
                                onClick={handleSubmissionButton}
                            >요청하기</button>
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
                            disabled={techBlogsQuery.isPending}
                        />
                    </div>
                </div>

                {techBlogsQuery.isPending ? (
                    <div className={styles.grid} aria-busy="true">
                        {Array.from({ length: SKELETON_COUNT }).map((_, i) => (
                            <article key={`s-${i}`} className={`${styles.card} ${styles.skeletonCard}`}>
                                <div className={styles.logoWrap}>
                                    <div className={`${styles.logo} ${styles.skeleton} ${styles.skeletonCircle}`} />
                                </div>
                                <div className={`${styles.skeletonLine} ${styles.skeleton}`} />
                                <div className={`${styles.skeletonLineShort} ${styles.skeleton}`} />
                                <div className={styles.subscription}>
                                    <div className={`${styles.skeletonBtn} ${styles.skeleton}`} />
                                    <span>·</span>
                                    <div className={`${styles.skeletonIcon} ${styles.skeleton}`} />
                                </div>
                            </article>
                        ))}
                    </div>
                ) : filteredBlogs.length === 0 ? (
                    <div className={styles.empty}>
                        <p>기술 블로그가 존재하지 않습니다.</p>
                    </div>
                ) : (
                    <div className={styles.grid}>
                        {filteredBlogs.map((blog) => (
                            <article
                                key={blog.id}
                                className={styles.card}
                                onClick={() => navigate(`/${blog.id}`)}
                            >
                                <div className={styles.logoWrap}>
                                    <img src={blog.icon} alt="thumbnail" className={styles.logo} />
                                </div>

                                <p className={styles.blogName}>{blog.title}</p>

                                <div className={styles.subscription}>
                                    <p className={styles.subscriptionCount}>
                                        구독자 {blog.subscriptionCount}명 · 게시글 {blog.postCount}개
                                    </p>
                                </div>

                                <div className={styles.subscription}>
                                    <button
                                        className={blog.subscribed ? styles.subscribing : styles.subButton}
                                        onClick={(e) => {
                                            stop(e)
                                            subscriptionToggle(blog)
                                        }}
                                        disabled={subToggle.isPending}
                                    >
                                        {blog.subscribed ? "구독중" : "구독"}
                                    </button>

                                    <span>·</span>
                                    <NotificationsNoneOutlinedIcon fontSize="small" />
                                </div>
                            </article>
                        ))}
                    </div>
                )}
            </section>
        </div>
    )
}

export default TechBlogsPage
