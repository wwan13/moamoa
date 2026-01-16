import styles from "./MySubscriptionPage.module.css"
import { useEffect, useMemo, useState } from "react"
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined"
import NotificationsOffOutlinedIcon from "@mui/icons-material/NotificationsOffOutlined"
import { showGlobalConfirm, showToast } from "../../api/client.js"
import useAuth from "../../auth/AuthContext.jsx"
import { useNavigate } from "react-router-dom"
import { useQueryClient } from "@tanstack/react-query"

import { useSubscribingTechBlogsQuery } from "../../queries/techBlog.queries.js"
import {
    useSubscriptionToggleMutation,
    useNotificationToggleMutation,
} from "../../queries/techBlogSubscription.queries.js"

const SKELETON_DELAY_MS = 300
const SKELETON_COUNT = 8

export default function MySubscriptionPage() {
    const { isLoggedIn } = useAuth()
    const navigate = useNavigate()
    const qc = useQueryClient()

    useEffect(() => {
        if (!isLoggedIn) navigate("/")
    }, [isLoggedIn, navigate])

    // ✅ 구독 블로그 조회 (query)
    const techBlogsQuery = useSubscribingTechBlogsQuery()
    const techBlogs = techBlogsQuery.data?.techBlogs ?? []

    console.log(techBlogs)

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

    // ✅ mutations
    const subToggle = useSubscriptionToggleMutation({ invalidateOnSuccess: false })
    const notiToggle = useNotificationToggleMutation()

    const isMutating = subToggle.isPending || notiToggle.isPending

    const { authScope } = useAuth()

    // ✅ 화면에서 optimistic 반영을 위해 캐시 직접 수정(페이지에서만)
    const patchBlog = (techBlogId, patcher) => {
        qc.setQueryData(["techBlogs", "subscribed", authScope], (old) => {
            if (!old?.techBlogs) return old
            return {
                ...old,
                techBlogs: old.techBlogs.map((b) => (b.id === techBlogId ? patcher(b) : b)),
            }
        })
    }

    const subscriptionToggle = async (blog) => {
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
        patchBlog(techBlogId, (b) => ({
            ...b,
            subscribed: !wasSubscribed,
            subscriptionCount: (b.subscriptionCount ?? 0) + (wasSubscribed ? -1 : 1),
        }))

        try {
            await subToggle.mutateAsync({ techBlogId })
            showToast(wasSubscribed ? "구독을 해제했어요." : "구독했어요.")
        } catch {
            // rollback
            patchBlog(techBlogId, (b) => ({
                ...b,
                subscribed: wasSubscribed,
                subscriptionCount: (b.subscriptionCount ?? 0) + (wasSubscribed ? 1 : -1),
            }))
            showToast("처리 중 오류가 발생했어요. 다시 시도해 주세요.")
        }
    }

    const notificationToggle = async (blog) => {
        const wasEnabled = !!blog.notificationEnabled
        const techBlogId = blog.id

        if (wasEnabled) {
            const ok = await showGlobalConfirm({
                title: "알림 해제",
                message: "이 기술 블로그 알림을 해제할까요?",
                confirmText: "해제",
                cancelText: "유지",
            })
            if (!ok) return
        }

        // optimistic
        patchBlog(techBlogId, (b) => ({
            ...b,
            notificationEnabled: !wasEnabled,
        }))

        try {
            await notiToggle.mutateAsync({ techBlogId })
            console.log("authScope", authScope)
            console.log("cache", qc.getQueryData(["techBlogs","subscribed",authScope]))
            showToast(wasEnabled ? "알람을 해제했어요." : "알람을 설정했어요.")
        } catch {
            // rollback
            patchBlog(techBlogId, (b) => ({
                ...b,
                notificationEnabled: wasEnabled,
            }))
            showToast("처리 중 오류가 발생했어요. 다시 시도해 주세요.")
        }
    }

    const list = useMemo(() => {
        if (showSkeleton) {
            return Array.from({ length: SKELETON_COUNT }).map((_, i) => (
                <div key={`s-${i}`} className={styles.item} aria-busy="true">
                    <div className={`${styles.iconWrap} ${styles.skeleton} ${styles.skeletonCircle}`}>
                        <div className={`${styles.icon}`} />
                    </div>
                    <div className={styles.infoWrap}>
                        <div className={styles.left}>
                            <div className={`${styles.skeletonLine} ${styles.skeleton}`} />
                            <div className={`${styles.skeletonLineShort} ${styles.skeleton}`} />
                            <div className={`${styles.skeletonLineShort} ${styles.skeleton}`} />
                        </div>
                        <div className={styles.right}>
                            <div className={`${styles.skeletonBtn} ${styles.skeleton}`} />
                            <div className={`${styles.skeletonBtnCircle} ${styles.skeleton}`} />
                        </div>
                    </div>
                </div>
            ))
        }

        return techBlogs.map((techBlog) => (
            <div key={techBlog.id} className={styles.item}>
                <div className={styles.iconWrap}>
                    <img src={techBlog.icon} alt="icon" className={styles.icon} />
                </div>

                <div className={styles.infoWrap}>
                    <div className={styles.left}>
                        <p className={styles.techBlogTitle}>{techBlog.title}</p>
                        <p className={styles.techBlogSub}>
                            구독자 {techBlog.subscriptionCount}명 · 게시글 {techBlog.postCount}개
                        </p>
                        <p className={styles.techBlogUrl}>{techBlog.blogUrl}</p>
                    </div>

                    <div className={styles.right}>
                        <button
                            className={techBlog.subscribed ? styles.subIngButton : styles.subButton}
                            onClick={() => subscriptionToggle(techBlog)}
                            disabled={isMutating}
                        >
                            {techBlog.subscribed ? "구독중" : "구독"}
                        </button>

                        <button
                            className={techBlog.notificationEnabled ? styles.alarmIngButton : styles.alarmButton}
                            onClick={() => notificationToggle(techBlog)}
                            disabled={isMutating}
                        >
                            {techBlog.notificationEnabled ? (
                                <NotificationsOffOutlinedIcon sx={{ fontSize: 18, color: "#A2A2A2", fontWeight: 800 }} />
                            ) : (
                                <NotificationsNoneOutlinedIcon sx={{ fontSize: 18, color: "#ffffff", fontWeight: 800 }} />
                            )}
                        </button>
                    </div>
                </div>
            </div>
        ))
    }, [showSkeleton, techBlogs, isMutating, subToggle.isPending])

    return (
        <div className={styles.wrap}>
            <p className={styles.title}>모든 구독 블로그</p>
            <div className={styles.itemsWrap}>{list}</div>
        </div>
    )
}