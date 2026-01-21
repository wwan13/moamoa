import styles from "./TechBlogItem.module.css"
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined"
import NotificationsOffOutlinedIcon from "@mui/icons-material/NotificationsOffOutlined"
import { useQueryClient } from "@tanstack/react-query"
import useAuth from "../../auth/AuthContext.jsx"
import { showGlobalConfirm, showToast } from "../../api/client.js"
import {
    useSubscriptionToggleMutation,
    useNotificationToggleMutation,
} from "../../queries/techBlogSubscription.queries.js"

export default function TechBlogItem({ techBlog, isSkeleton = false }) {
    const qc = useQueryClient()
    const { authScope, isLoggedIn, openLogin } = useAuth()

    const subToggle = useSubscriptionToggleMutation({ invalidateOnSuccess: false })
    const notiToggle = useNotificationToggleMutation()
    const isMutating = subToggle.isPending || notiToggle.isPending

    const patchBlog = (techBlogId, patcher) => {
        qc.setQueriesData(
            { queryKey: ["techBlogs"], exact: false },
            (old) => {
                if (!old?.techBlogs) return old
                return {
                    ...old,
                    techBlogs: old.techBlogs.map((b) => (b.id === techBlogId ? patcher(b) : b)),
                }
            }
        )
    }

    const subscriptionToggle = async () => {
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

        const wasSubscribed = !!techBlog.subscribed
        const techBlogId = techBlog.id

        if (wasSubscribed) {
            const ok = await showGlobalConfirm({
                title: "구독 해제",
                message: "이 기술 블로그 구독을 해제할까요?",
                confirmText: "해제",
                cancelText: "유지",
            })
            if (!ok) return
        }

        patchBlog(techBlogId, (b) => ({
            ...b,
            subscribed: !wasSubscribed,
            subscriptionCount: (b.subscriptionCount ?? 0) + (wasSubscribed ? -1 : 1),
        }))

        try {
            await subToggle.mutateAsync({ techBlogId })
            showToast(wasSubscribed ? "구독을 해제했어요." : "구독했어요.")
        } catch {
            patchBlog(techBlogId, (b) => ({
                ...b,
                subscribed: wasSubscribed,
                subscriptionCount: (b.subscriptionCount ?? 0) + (wasSubscribed ? 1 : -1),
            }))
            showToast("처리 중 오류가 발생했어요. 다시 시도해 주세요.")
        }
    }

    const notificationToggle = async () => {
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

        const wasEnabled = !!techBlog.notificationEnabled
        const techBlogId = techBlog.id

        if (wasEnabled) {
            const ok = await showGlobalConfirm({
                title: "알림 해제",
                message: "이 기술 블로그 알림을 해제할까요?",
                confirmText: "해제",
                cancelText: "유지",
            })
            if (!ok) return
        }

        patchBlog(techBlogId, (b) => ({
            ...b,
            notificationEnabled: !wasEnabled,
        }))

        try {
            await notiToggle.mutateAsync({ techBlogId })
            showToast(wasEnabled ? "알람을 해제했어요." : "알람을 설정했어요.")
        } catch {
            patchBlog(techBlogId, (b) => ({
                ...b,
                notificationEnabled: wasEnabled,
            }))
            showToast("처리 중 오류가 발생했어요. 다시 시도해 주세요.")
        }
    }

    if (isSkeleton) {
        return (
            <div className={styles.item} aria-busy="true">
                <div className={`${styles.iconWrap} ${styles.skeleton} ${styles.skeletonCircle}`}>
                    <div className={styles.icon} />
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
        )
    }

    return (
        <div className={styles.item}>
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
                        onClick={subscriptionToggle}
                        disabled={isMutating}
                    >
                        {techBlog.subscribed ? "구독중" : "구독"}
                    </button>

                    {techBlog.subscribed && (
                        <button
                            className={techBlog.notificationEnabled ? styles.alarmIngButton : styles.alarmButton}
                            onClick={notificationToggle}
                            disabled={isMutating}
                        >
                            {techBlog.notificationEnabled ? (
                                <NotificationsOffOutlinedIcon sx={{ fontSize: 18, color: "#A2A2A2", fontWeight: 800 }} />
                            ) : (
                                <NotificationsNoneOutlinedIcon sx={{ fontSize: 18, color: "#ffffff", fontWeight: 800 }} />
                            )}
                        </button>
                    )}
                </div>
            </div>
        </div>
    )
}