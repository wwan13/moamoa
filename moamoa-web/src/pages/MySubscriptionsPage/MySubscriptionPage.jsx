import styles from "./MySubscriptionPage.module.css"
import {useEffect, useState} from "react";
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined";
import NotificationsOffOutlinedIcon from '@mui/icons-material/NotificationsOffOutlined';
import {subscribingTechBlogsApi} from "../../api/techblog.api.js";
import {showGlobalConfirm, showToast} from "../../api/client.js";
import {notificationToggleApi, subscriptionToggleApi} from "../../api/subscription.api.js";

export default function MySubscriptionPage() {
    const [techBlogs, setTechBlogs] = useState([])

    useEffect(() => {
        const fetchSubscriptions = async () => {
            try {
                const subsRes = await subscribingTechBlogsApi()
                setTechBlogs(subsRes.techBlogs)
            } catch (e) {
            }
        }
        fetchSubscriptions()
    }, []);

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

        setTechBlogs((prev) =>
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
            setTechBlogs((prev) =>
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

        setTechBlogs((prev) =>
            prev.map((b) =>
                b.id === techBlogId
                    ? {
                        ...b,
                        notificationEnabled: !wasEnabled,
                    }
                    : b
            )
        )

        try {
            await notificationToggleApi(techBlogId)
            showToast(wasEnabled ? "알람을 해제했어요." : "알람을 설정했어요.")
        } catch (e) {
            // ✅ rollback
            setTechBlogs((prev) =>
                prev.map((b) =>
                    b.id === techBlogId
                        ? {
                            ...b,
                            notificationEnabled: wasEnabled,
                        }
                        : b
                )
            )
            showToast("처리 중 오류가 발생했어요. 다시 시도해 주세요.")
        }
    }

    return (
        <div className={styles.wrap}>
            <p className={styles.title}>모든 구독 블로그</p>
            <div className={styles.itemsWrap}>
                {techBlogs.map((techBlog) => {
                    return (
                        <div key={techBlog.id} className={styles.item}>
                            <div className={styles.iconWrap}>
                                <img
                                    src={techBlog.icon}
                                    alt="icon"
                                    className={styles.icon}
                                />
                            </div>
                            <div className={styles.infoWrap}>
                                <div className={styles.left}>
                                    <p className={styles.techBlogTitle}>{techBlog.title}</p>
                                    <p className={styles.techBlogSub}>구독자 {techBlog.subscriptionCount}명 · 게시글 {techBlog.postCount}개</p>
                                    <p className={styles.techBlogUrl}>{techBlog.blogUrl}</p>
                                </div>
                                <div className={styles.right}>
                                    <button
                                        className={techBlog.subscribed ? styles.subIngButton : styles.subButton}
                                        onClick={() => subscriptionToggle(techBlog)}
                                    >
                                        {techBlog.subscribed ? "구독중" : "구독"}
                                    </button>
                                    <button
                                        className={techBlog.notificationEnabled ? styles.alarmIngButton : styles.alarmButton}
                                        onClick={() => notificationToggle(techBlog)}
                                    >
                                        {
                                            techBlog.notificationEnabled ?
                                                <NotificationsOffOutlinedIcon sx={{ fontSize: 18, color: "#A2A2A2", fontWeight: 800 }}/> :
                                                <NotificationsNoneOutlinedIcon sx={{ fontSize: 18, color: "#ffffff", fontWeight: 800 }}/>
                                        }
                                    </button>
                                </div>
                            </div>
                        </div>
                    )
                })}
            </div>
        </div>
    )
}