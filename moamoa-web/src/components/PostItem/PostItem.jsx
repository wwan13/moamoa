import styles from "./PostItem.module.css"
import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder"
import BookmarkIcon from "@mui/icons-material/Bookmark"
import VisibilityIcon from "@mui/icons-material/Visibility"
import {formatRelativeDate} from "../../utils/date.js"
import {useEffect, useState} from "react"
import useAuth from "../../auth/AuthContext.jsx"
import {useNavigate} from "react-router-dom"
import {showToast} from "../../api/client.js"
import {useBookmarkToggleMutation} from "../../queries/bookmark.queries.js"
import {useIncreasePostViewCountMutation} from "../../queries/post.queries.js"

export default function PostItem({post, isBlogDetail, isLoading = false}) {
    const navigate = useNavigate()
    const {isLoggedIn, openLogin} = useAuth()

    const bookmarkToggle = useBookmarkToggleMutation()
    const increaseView = useIncreasePostViewCountMutation()

    const [bookmarked, setBookmarked] = useState(post?.isBookmarked ?? false)
    const [bookmarkCount, setBookmarkCount] = useState(post?.bookmarkCount ?? 0)
    const [viewCount, setViewCount] = useState(post?.viewCount ?? 0)

    useEffect(() => {
        if (!post) return
        setBookmarked(!!post.isBookmarked)
        setBookmarkCount(post.bookmarkCount ?? 0)
        setViewCount(post.viewCount ?? 0)
    }, [post?.id])

    const stop = (e) => {
        e.preventDefault()
        e.stopPropagation()
    }

    const onOpenPost = (postId, postUrl) => {
        window.open(postUrl, "_blank", "noopener,noreferrer")

        // ✅ 조회수는 mutation으로 "조용히" 전송
        increaseView.mutate({postId})
        setViewCount((v) => v + 1)
    }

    const onToggleBookmark = async (postId) => {
        if (!isLoggedIn) {
            openLogin()
            return
        }
        if (bookmarkToggle.isPending) return

        const next = !bookmarked

        // ✅ optimistic update
        setBookmarked(next)
        setBookmarkCount((c) => c + (next ? 1 : -1))

        try {
            const res = await bookmarkToggle.mutateAsync({postId})
            const finalBookmarked = !!res?.bookmarked

            setBookmarked(finalBookmarked)

            if (finalBookmarked) showToast("북마크 되었습니다.")
            else showToast("북마크 해제하였습니다.")
        } catch {
            // rollback
            setBookmarked(!next)
            setBookmarkCount((c) => c + (next ? -1 : 1))
        }
    }

    // ✅ 스켈레톤 (조회 로딩일 때)
    if (isLoading) {
        return (
            <article className={`${styles.card} ${styles.skeletonCard}`} aria-busy="true">
                <div className={styles.thumbnailWrap}>
                    <div className={`${styles.thumbnailImage} ${styles.skeleton}`}/>
                    <div className={`${styles.bookmarkButton} ${styles.skeletonCircle}`}/>
                </div>

                <div className={styles.postInfo}>
                    <div>
                        <div className={`${styles.title} ${styles.skeletonLine}`}/>
                        <div className={`${styles.summary} ${styles.skeletonLine}`}/>
                        <div className={`${styles.summary} ${styles.skeletonLineShort}`}/>
                    </div>

                    <div className={styles.metaRow}>
                        <div className={styles.source}>
                            <div className={`${styles.skeletonLineShort} ${styles.skeleton}`}/>
                        </div>
                        <div className={styles.stats}>
                            <div className={`${styles.skeletonLineShort} ${styles.skeleton}`}/>
                        </div>
                    </div>
                </div>
            </article>
        )
    }

    if (!post) return null

    return (
        <article
            className={styles.card}
            onClick={() => onOpenPost(post.id, post.url)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === "Enter" && onOpenPost(post.id, post.url)}
        >
            <div className={styles.thumbnailWrap}>
                <img
                    src={post.thumbnail}
                    alt="thumbnail"
                    className={styles.thumbnailImage}
                />

                <button
                    type="button"
                    className={styles.bookmarkButton}
                    disabled={bookmarkToggle.isPending}
                    onClick={(e) => {
                        stop(e)
                        onToggleBookmark(post.id)
                    }}
                    aria-label={bookmarked ? "북마크 해제" : "북마크"}
                >
                    {bookmarked ? (
                        <BookmarkIcon sx={{fontSize: 16, color: "#90AB8B", fontWeight: 800}}/>
                    ) : (
                        <BookmarkBorderIcon sx={{fontSize: 16, color: "#90AB8B", fontWeight: 800}}/>
                    )}
                </button>
            </div>

            <div className={styles.postInfo}>
                <div>
                    <div className={styles.title}>{post.title}</div>
                    <div className={styles.summary}>{post.description}</div>
                </div>

                <div className={styles.metaRow}>
                    <div className={styles.source}>
                        {!isBlogDetail && (
                            <>
                                <div
                                    className={styles.sourceLink}
                                    onClick={(e) => {
                                        stop(e)
                                        navigate(`/${post.techBlog.key}`)
                                    }}
                                >
                                    <img
                                        src={post.techBlog?.icon || ""}
                                        alt={post.techBlog?.title || ""}
                                        className={styles.sourceIcon}
                                    />
                                    <span className={styles.sourceName}>{post.techBlog?.title || ""}</span>
                                </div>
                                <span className={styles.dot}>·</span>
                            </>
                        )}

                        <time className={styles.date}>{formatRelativeDate(post.publishedAt)}</time>
                    </div>

                    <div className={styles.stats}>
            <span className={styles.stat}>
              <BookmarkBorderIcon fontSize="inherit"/>
                {bookmarkCount}
            </span>

                        <span className={styles.dot}>·</span>

                        <span className={styles.stat}>
              <VisibilityIcon fontSize="inherit"/>
                            {viewCount}
            </span>
                    </div>
                </div>
            </div>
        </article>
    )
}