import styles from "./TechBlogDetailPage.module.css"
import useAuth from "../../auth/AuthContext.jsx"
import { useEffect, useMemo, useState } from "react"
import { useNavigate, useParams } from "react-router-dom"
import { showGlobalAlert, showGlobalConfirm, showToast } from "../../api/client.js"
import PostList from "../../components/PostList/PostList.jsx"
import { usePagingQuery } from "../../hooks/usePagingQuery.js"
import { useQueryClient } from "@tanstack/react-query"

import {useSubscribingTechBlogsQuery, useTechBlogByKeyQuery} from "../../queries/techBlog.queries.js"
import { usePostsByTechBlogKeyQuery } from "../../queries/post.queries.js"
import { useSubscriptionToggleMutation } from "../../queries/techBlogSubscription.queries.js"

export default function TechBlogDetailPage() {
    const { isLoggedIn, openLogin } = useAuth()
    const navigate = useNavigate()
    const { key } = useParams()
    const qc = useQueryClient()

    const { page, setPage } = usePagingQuery()

    // ✅ tech blog detail (query)
    const techBlogQuery = useTechBlogByKeyQuery({ key })

    // ✅ posts by tech blog (query)
    const postsQuery = usePostsByTechBlogKeyQuery(
        { page, techBlogKey: key }
    )

    // ✅ my subscribing blogs (query, only when logged in)
    const subsQuery = useSubscribingTechBlogsQuery()

    // ✅ subscription toggle (mutation)
    const subToggle = useSubscriptionToggleMutation()

    // ✅ techBlog data
    const techBlog = techBlogQuery.data
    const postsRes = postsQuery.data

    const posts = postsRes?.posts ?? []
    const totalPages = postsRes?.meta?.totalPages ?? 0
    const totalPostCount = postsRes?.meta?.totalCount ?? 0

    // ✅ 구독 여부: techBlog가 로드된 다음에 subsQuery에서 판별
    const subscribed = useMemo(() => {
        if (!isLoggedIn) return false
        if (!techBlog) return false

        const subs = subsQuery.data.techBlogs ?? []
        return Array.isArray(subs)
            ? subs.some(
                (s) =>
                    s.techBlogId === techBlog.id ||
                    s.techBlog?.id === techBlog.id ||
                    s.id === techBlog.id
            )
            : false
    }, [isLoggedIn, subsQuery.data, techBlog])

    // ✅ 구독자 수는 detail에서 보이니까 optimistic을 위해 로컬로만 들고감
    const [subCount, setSubCount] = useState(0)
    useEffect(() => {
        if (techBlog?.subscriptionCount != null) setSubCount(techBlog.subscriptionCount)
    }, [techBlog?.id])

    // ✅ 에러 처리 (쿼리 실패 시)
    useEffect(() => {
        if (techBlogQuery.isError) {
            ;(async () => {
                await showGlobalAlert("기술 블로그 정보를 불러오지 못했어요.")
                navigate(-1)
            })()
        }
    }, [techBlogQuery.isError])

    // ✅ top scroll (데이터 바뀔 때)
    useEffect(() => {
        window.scrollTo({ top: 0, behavior: "smooth" })
    }, [key, page])

    const onSubButtonToggle = async () => {
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
        if (!techBlog) return
        if (subToggle.isPending) return

        if (subscribed) {
            const ok = await showGlobalConfirm({
                title: "구독 해제",
                message: "이 기술 블로그 구독을 해제할까요?",
                confirmText: "해제",
                cancelText: "유지",
            })
            if (!ok) return
        }

        const wasSubscribed = subscribed

        // optimistic: 구독자 수만 즉시 반영
        setSubCount((c) => c + (wasSubscribed ? -1 : 1))

        try {
            await subToggle.mutateAsync({ techBlogId: techBlog.id })

            // 구독 목록/구독 기반 posts 등에 영향 → invalidate는 mutation 훅에 이미 들어있어도 OK
            qc.invalidateQueries({ queryKey: ["techBlogs", "subscribed"] })

            showToast(wasSubscribed ? "구독을 해제했어요." : "구독했어요.")
        } catch {
            // rollback
            setSubCount((c) => c + (wasSubscribed ? 1 : -1))
            showToast("처리 중 오류가 발생했어요. 다시 시도해 주세요.")
        }
    }

    return (
        <>
            <div className={styles.blogInfo}>
                <div className={styles.iconWrap}>
                    {techBlogQuery.isPending ? (
                        <div className={`${styles.icon} ${styles.skeleton} ${styles.skeletonIcon}`} />
                    ) : (
                        <img src={techBlog?.icon || ""} alt="icon" className={styles.icon} />
                    )}
                </div>

                <div className={styles.detail}>
                    <div className={styles.blogDetail}>
                        {techBlogQuery.isPending ? (
                            <>
                                <div className={`${styles.skeletonLine} ${styles.skeleton}`} />
                                <div className={`${styles.skeletonLineShort} ${styles.skeleton}`} />
                                <div className={`${styles.skeletonLineShort} ${styles.skeleton}`} />
                            </>
                        ) : (
                            <>
                                <p className={styles.blogTitle}>{techBlog?.title || ""}</p>
                                <p className={styles.blogStats}>
                                    구독자 {subCount}명 · 게시글 {totalPostCount}개
                                </p>
                                <button
                                    className={styles.blogLink}
                                    onClick={() => techBlog?.blogUrl && window.open(techBlog.blogUrl)}
                                >
                                    {techBlog?.blogUrl || ""}
                                </button>
                            </>
                        )}
                    </div>

                    {/* mutation이므로 스켈레톤 X, 버튼 상태만 */}
                    <button
                        className={subscribed ? styles.subIngButton : styles.subButton}
                        onClick={onSubButtonToggle}
                        disabled={subToggle.isPending || techBlogQuery.isPending}
                    >
                        {subscribed ? "구독중" : "구독"}
                    </button>
                </div>
            </div>

            <div className={styles.wrap}>
                <div className={styles.posts}>
                    <PostList
                        posts={posts}
                        totalPages={totalPages}
                        type="subscribed"
                        isBlogDetail
                        emptyMessage="기술 블로그에 게시글이 존재하지 않습니다."
                        isLoading={postsQuery.isPending}
                    />
                </div>
            </div>
        </>
    )
}