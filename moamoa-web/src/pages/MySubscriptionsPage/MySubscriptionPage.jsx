import styles from "./MySubscriptionPage.module.css"
import {useEffect, useState} from "react";
import {subscribingBlogsApi} from "../../api/subscription.api.js";

export default function MySubscriptionPage() {
    const [techBlogs, setTechBlogs] = useState([])

    useEffect(() => {
        const fetchSubscriptions = async () => {
            try {
                const subsRes = await subscribingBlogsApi()
                setTechBlogs(subsRes)
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
                                <div>
                                    <p className={styles.techBlogTitle}>{techBlog.title}</p>
                                    <p className={styles.techBlogSub}>구독자 {techBlog.subscriptionCount}명 · 게시글 1개</p>
                                </div>
                                <div>
                                    <button>구독중</button>
                                </div>
                            </div>
                        </div>
                    )
                })}
            </div>
        </div>
    )
}