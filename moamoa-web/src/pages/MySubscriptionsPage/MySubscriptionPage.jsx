import styles from "./MySubscriptionPage.module.css"
import {useEffect, useState} from "react";
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined";
import {subscribingTechBlogsApi} from "../../api/techblog.api.js";

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
                                    <button className={styles.subIngButton}>구독중</button>
                                    <button className={styles.alarmIngButton}>
                                        <NotificationsNoneOutlinedIcon sx={{ fontSize: 18, color: "#A2A2A2", fontWeight: 800 }}/>
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