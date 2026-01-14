import { useEffect, useMemo, useState } from "react"
import styles from "./TechBlogsPage.module.css"
import { useCountUp } from "../../hooks/useCountUp.js"
import useAuth from "../../auth/AuthContext.jsx"
import { showGlobalConfirm, showToast } from "../../api/client.js"
import { useNavigate } from "react-router-dom"
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined"
import { useQueryClient } from "@tanstack/react-query"

import { useTechBlogsQuery } from "../../queries/techBlog.queries.js"
import { useSubscribingBlogsQuery, useSubscriptionToggleMutation } from "../../queries/techBlogSubscription.queries.js"

const SKELETON_DELAY_MS = 300
const SKELETON_COUNT = 12

export default function TechBlogsPage() {
    const navigate = useNavigate()
    const { isLoggedIn, openLogin } = useAuth()
    const qc = useQueryClient()

    const [search, setSearch] = useState("")

    // ✅ tech blogs list (query)
    const techBlogsQuery = useTechBlogsQuery()

    // ✅ my subscriptions (query) - 로그인 시에만
    const subsQuery = useSubscribingBlogsQuery({ enabled: isLoggedIn })

    // ✅ 1초 이상일 때만 스켈레톤
    const [showSkeleton, setShowSkeleton] = useState(false)
    useEffect(() => {
        let timer = null
        if (techBlogsQuery.isPending) {
            timer = setTimeout(() => setShowSkeleton(true), SKELETON_DELAY_MS)
        } else {
            setShowSkeleton(false)
        }
        return () => timer && clearTimeout(timer)
    }, [techBlogsQuery.isPending])

    const rawBlogs = techBlogsQuery.data?.techBlogs ?? []
    const totalCount = techBlogsQuery.data?.meta?.totalCount ?? rawBlogs.length
    const animated = useCountUp(totalCount, 900)

    // ✅ 서버가 list에 subscribed를 주지 않는 경우 대비: 내 구독 목록으로 merge
    const mergedBlogs = useMemo(() => {
        const subs = subsQuery.data ?? []
        const subscribedSet = new Set(
            Array.isArray(subs)
                ? subs.map((s) => s.techBlogId ?? s.techBlog?.id ?? s.id).filter(Boolean)
                : []
        )

        return rawBlogs.map((b) => ({
            ...b,
            subscribed: b.subscribed ?? subscribedSet.has(b.id),
        }))
    }, [rawBlogs, subsQuery.data])

    const filteredBlogs = useMemo(() => {
        const q = search.trim().toLowerCase()
        if (!q) return mergedBlogs

        return mergedBlogs.filter((b) => {
            const title = (b.title ?? "").toLowerCase()
            const key = (b.key ?? "").toLowerCase()
            return title.includes(q) || key.includes(q)
        })
    }, [mergedBlogs, search])

    // ✅ subscribe toggle (mutation)
    const subToggle = useSubscriptionToggleMutation()

    // ✅ optimistic 업데이트를 위해 techBlogs 캐시 수정
    const patchTechBlog = (techBlogId, patcher) => {
        qc.setQueryData(["techBlogs"], (old) => {
            if (!old?.techBlogs) return old
            return {
                ...old,
                techBlogs: old.techBlogs.map((b) => (b.id === techBlogId ? patcher(b) : b)),
            }
        })
    }

    const subscriptionToggle = async (blog) => {
        if (!isLoggedIn) {
            openLogin?.()
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
                            <span className={styles.count}>{animated}</span>
                            개의 기술 블로그를
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
                            disabled={showSkeleton}
                        />
                    </div>
                </div>

                {showSkeleton ? (
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
                                onClick={() => navigate(`/${blog.key}`)}
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
                                        {subToggle.isPending ? "처리 중..." : blog.subscribed ? "구독중" : "구독"}
                                    </button>

                                    <span>·</span>
                                    <NotificationsNoneOutlinedIcon fontSize="10px" />
                                </div>
                            </article>
                        ))}
                    </div>
                )}
            </section>
        </div>
    )
}