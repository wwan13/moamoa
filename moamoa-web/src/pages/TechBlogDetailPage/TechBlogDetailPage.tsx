import styles from "./TechBlogDetailPage.module.css"
import useAuth from "../../auth/useAuth"
import { useEffect, useMemo } from "react"
import { useNavigate, useParams } from "react-router-dom"
import { showGlobalAlert, showGlobalConfirm, showToast } from "../../api/client"
import PostList from "../../components/PostList/PostList"
import { usePagingQuery } from "../../hooks/usePagingQuery"
import { useQueryClient } from "@tanstack/react-query"

import { useTechBlogByIdQuery } from "../../queries/techBlog.queries"
import { usePostsByTechBlogIdQuery } from "../../queries/post.queries"
import {
    useNotificationToggleMutation,
    useSubscriptionToggleMutation,
} from "../../queries/techBlogSubscription.queries"
import NotificationsOffOutlinedIcon from "@mui/icons-material/NotificationsOffOutlined"
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined"
import type { TechBlogList, TechBlogSummary } from "../../api/techBlog.api"

const TechBlogDetailPage = () => {
    const { isLoggedIn, openLogin, authScope, publicScope } = useAuth()
    const scope = authScope ?? publicScope

    const navigate = useNavigate()
    const { techBlogId } = useParams()
    const qc = useQueryClient()
    const { page } = usePagingQuery()

    // ✅ queries
    const techBlogQuery = useTechBlogByIdQuery({ techBlogId })
    const postsQuery = usePostsByTechBlogIdQuery({ page, techBlogId })

    // ✅ mutations
    const subToggle = useSubscriptionToggleMutation()
    const notiToggle = useNotificationToggleMutation()

    // ✅ data
    const techBlog = techBlogQuery.data
    const postsRes = postsQuery.data

    const posts = postsRes?.posts ?? []
    const totalPages = postsRes?.meta?.totalPages ?? 0
    const totalPostCount = postsRes?.meta?.totalCount ?? 0

    // ✅ 단일 소스: query 데이터만 사용
    const subscribed = !!techBlog?.subscribed
    const notificationEnabled = !!techBlog?.notificationEnabled
    const subCount = techBlog?.subscriptionCount ?? 0

    // ✅ techBlog queryKey: 훅과 동일하게 유지(현재 코드 기준)
    const techBlogQk = useMemo(() => ["techBlog", scope, techBlogId], [scope, techBlogId])

    const patchTechBlogDetail = (patcher: (blog: TechBlogSummary) => TechBlogSummary) => {
        qc.setQueryData(techBlogQk, (old: unknown) => {
            if (!old) return old
            return patcher(old as TechBlogSummary)
        })
    }

    const patchBlogList = (targetId: number, patcher: (blog: TechBlogSummary) => TechBlogSummary) => {
        qc.setQueriesData({ queryKey: ["techBlogs"], exact: false }, (old: unknown) => {
            const cache = old as TechBlogList | undefined
            if (!cache?.techBlogs) return old
            return {
                ...cache,
                techBlogs: cache.techBlogs.map((b) => (b.id === targetId ? patcher(b) : b)),
            }
        })
    }

    const patchBoth = (targetId: number, patcher: (blog: TechBlogSummary) => TechBlogSummary) => {
        patchTechBlogDetail(patcher)
        patchBlogList(targetId, patcher)
    }

    // ✅ 에러 처리
    useEffect(() => {
        if (!techBlogQuery.isError) return
            ;(async () => {
            await showGlobalAlert("기술 블로그 정보를 불러오지 못했어요.")
            navigate(-1)
        })()
    }, [techBlogQuery.isError, navigate])

    // ✅ top scroll
    useEffect(() => {
        window.scrollTo({ top: 0, behavior: "smooth" })
    }, [techBlogId, page])

    const askLogin = async () => {
        if (isLoggedIn) return true
        const ok = await showGlobalConfirm({
            title: "로그인",
            message: "로그인이 필요한 기능입니다. 로그인 하시겠습니까?",
            confirmText: "로그인",
        })
        if (!ok) return false
        openLogin()
        return false
    }

    const onSubButtonToggle = async () => {
        const okLogin = await askLogin()
        if (!okLogin) return

        if (!techBlog) return
        if (subToggle.isPending) return

        const techBlogIdNum = techBlog.id
        const wasSubscribed = !!techBlog.subscribed

        if (wasSubscribed) {
            const ok = await showGlobalConfirm({
                title: "구독 해제",
                message: "이 기술 블로그 구독을 해제할까요?",
                confirmText: "해제",
                cancelText: "유지",
            })
            if (!ok) return
        }

        // ✅ optimistic: 상세/목록 동시 반영
        patchBoth(techBlogIdNum, (b) => ({
            ...b,
            subscribed: !wasSubscribed,
            subscriptionCount: (b.subscriptionCount ?? 0) + (wasSubscribed ? -1 : 1),
            // ✅ 구독 해제면 알림 off / 구독이면 알림 on
            notificationEnabled: wasSubscribed ? false : true,
        }))

        try {
            await subToggle.mutateAsync({ techBlogId: techBlogIdNum })

            // 서버가 count/상태를 다시 계산하는 구조면 최소 refetch로 정합성 확보
            qc.invalidateQueries({ queryKey: techBlogQk, refetchType: "inactive" })
            qc.invalidateQueries({ queryKey: ["techBlogs"], refetchType: "inactive" })

            showToast(wasSubscribed ? "구독을 해제했어요." : "구독했어요.")
        } catch {
            // ✅ rollback: 상세/목록 동시 복원
            patchBoth(techBlogIdNum, (b) => ({
                ...b,
                subscribed: wasSubscribed,
                subscriptionCount: (b.subscriptionCount ?? 0) + (wasSubscribed ? 1 : -1),
                // notificationEnabled는 원래 값이 뭔지 모를 수 있어 안전하게 refetch에 맡김
            }))

            qc.invalidateQueries({ queryKey: techBlogQk })
            showToast("처리 중 오류가 발생했어요. 다시 시도해 주세요.")
        }
    }

    const onNotificationButtonToggle = async () => {
        const okLogin = await askLogin()
        if (!okLogin) return

        if (!techBlog) return
        if (notiToggle.isPending) return
        if (!techBlog.subscribed) return

        const techBlogIdNum = techBlog.id
        const wasEnabled = !!techBlog.notificationEnabled

        if (wasEnabled) {
            const ok = await showGlobalConfirm({
                title: "알림 해제",
                message: "이 기술 블로그 알림을 해제할까요?",
                confirmText: "해제",
                cancelText: "유지",
            })
            if (!ok) return
        }

        // ✅ optimistic: 상세/목록 동시 반영
        patchBoth(techBlogIdNum, (b) => ({
            ...b,
            notificationEnabled: !wasEnabled,
        }))

        try {
            await notiToggle.mutateAsync({ techBlogId: techBlogIdNum })

            qc.invalidateQueries({ queryKey: techBlogQk, refetchType: "inactive" })
            qc.invalidateQueries({ queryKey: ["techBlogs"], refetchType: "inactive" })

            showToast(wasEnabled ? "알림을 해제했어요." : "알림을 설정했어요.")
        } catch {
            // ✅ rollback
            patchBoth(techBlogIdNum, (b) => ({
                ...b,
                notificationEnabled: wasEnabled,
            }))

            qc.invalidateQueries({ queryKey: techBlogQk })
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

                    <div className={styles.buttonWrap}>
                        <button
                            className={subscribed ? styles.subIngButton : styles.subButton}
                            onClick={onSubButtonToggle}
                            disabled={subToggle.isPending || techBlogQuery.isPending}
                        >
                            {subscribed ? "구독중" : "구독"}
                        </button>

                        {subscribed && techBlog && !subToggle.isPending && (
                            <button
                                className={notificationEnabled ? styles.alarmIngButton : styles.alarmButton}
                                onClick={onNotificationButtonToggle}
                                disabled={notiToggle.isPending || techBlogQuery.isPending}
                            >
                                {notificationEnabled ? (
                                    <NotificationsOffOutlinedIcon sx={{ fontSize: 18, color: "#A2A2A2", fontWeight: 800 }} />
                                ) : (
                                    <NotificationsNoneOutlinedIcon sx={{ fontSize: 18, color: "#ffffff", fontWeight: 800 }} />
                                )}
                            </button>
                        )}
                    </div>
                </div>
            </div>

            <div className={styles.wrap}>
                <div className={styles.posts}>
                    <PostList
                        posts={posts}
                        totalPages={totalPages}
                        type="blogDetail"
                        isBlogDetail
                        emptyMessage="기술 블로그에 게시글이 존재하지 않습니다."
                        isLoading={postsQuery.isPending}
                    />
                </div>
            </div>
        </>
    )
}

export default TechBlogDetailPage
