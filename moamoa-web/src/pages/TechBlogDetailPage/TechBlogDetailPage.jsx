import styles from "./TechBlogDetailPage.module.css"
import useAuth from "../../auth/AuthContext.jsx"
import {useEffect, useState} from "react";
import {useNavigate, useParams} from "react-router-dom";
import {findByTechBlogKeyApi} from "../../api/techblog.api.js";
import {showGlobalAlert, showGlobalConfirm, showToast} from "../../api/client.js";
import {postsApi} from "../../api/post.api.js";
import PostList from "../../components/PostList/PostList.jsx";
import {usePagingQuery} from "../../hooks/usePagingQuery.js";
import {subscribingBlogsApi, subscriptionToggleApi} from "../../api/subscription.api.js";

export default function TechBlogDetailPage() {
    const { isLoggedIn } = useAuth()
    const navigate = useNavigate()
    const { key } = useParams()
    const [techBlogId, setTechBlogId] = useState(0)

    const [techBlog, setTechBlog] = useState("")
    const [totalPostCount, setTotalPostCount] = useState(0)
    const [subCount, setSubCount] = useState(0)
    const [posts, setPosts] = useState([])

    const { page, size, setPage } = usePagingQuery()
    const [totalPages, setTotalPages] = useState(1)

    const [sub, setSub] = useState(false)

    useEffect(() => {
        const fetch = async () => {
            try {
                const techBlogRes = await findByTechBlogKeyApi(key)
                setTechBlog(techBlogRes)
                setTechBlogId(techBlogRes.id)
                setSubCount(techBlogRes.subscriptionCount)

                const postsPromise = postsApi({
                    page: page,
                    techBlogId: techBlogRes.id,
                })

                const subsPromise = isLoggedIn ? subscribingBlogsApi() : Promise.resolve([])

                const [postsRes, subsRes] = await Promise.all([postsPromise, subsPromise])

                setPosts(postsRes.posts)
                setTotalPages(postsRes.meta.totalPages)
                setTotalPostCount(postsRes.meta.totalCount)

                // ✅ 구독 여부 판별 (subsRes 구조에 맞게 id 경로만 맞추면 됨)
                const subscribed = Array.isArray(subsRes)
                    ? subsRes.some((s) => s.techBlogId === techBlogRes.id || s.techBlog?.id === techBlogRes.id || s.id === techBlogRes.id)
                    : false

                setSub(isLoggedIn ? subscribed : false)

                window.scrollTo({ top: 0, behavior: "smooth" })
            } catch (e) {
                await showGlobalAlert(e.message)
                navigate(-1)
            }
        }

        fetch()
    }, [isLoggedIn, key, page])

    const onSubButtonToggle = async () => {
        if (sub) {
            const ok = await showGlobalConfirm({
                title: "구독 해제",
                message: "이 기술 블로그 구독을 해제할까요?",
                confirmText: "해제",
                cancelText: "유지",
            })

            if (!ok) return
        }

        const res = await subscriptionToggleApi(techBlogId)
        setSub(res.subscribing)

        if (res.subscribing) {
            setSubCount((c) => c + 1)
            showToast("구독했어요.")
        } else {
            setSubCount((c) => c - 1)
            showToast("구독을 해제했어요.")
        }
    }

    return (
        <>
            <div className={styles.blogInfo}>
                <div className={styles.iconWrap}>
                    <img src={techBlog.icon} alt="icon" className={styles.icon}/>
                </div>
                <div className={styles.detail}>
                    <div className={styles.blogDetail}>
                        <p className={styles.blogTitle}>{techBlog.title}</p>
                        <p className={styles.blogStats}>구독자 {subCount}명 · 게시글 {totalPostCount}개</p>
                        <button className={styles.blogLink}
                                onClick={() => window.open(techBlog.blogUrl)}>{techBlog.blogUrl}</button>
                    </div>
                    {sub ?
                        <button
                            className={styles.subIngButton}
                            onClick={onSubButtonToggle}
                        >구독 해제</button>
                        :
                        <button
                            className={styles.subButton}
                            onClick={onSubButtonToggle}
                        >구독</button>
                    }
                </div>
            </div>
            <div className={styles.wrap}>

                <div>
                    <div className={styles.posts}>
                        <PostList
                            posts={posts}
                            page={page}
                            totalPages={totalPages}
                            onChangePage={setPage}
                            isBlogDetail={true}
                        />
                    </div>
                </div>
            </div>
        </>
    )
}