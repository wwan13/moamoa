import styles from "./PostItem.module.css"
import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder"
import BookmarkIcon from "@mui/icons-material/Bookmark"
import VisibilityIcon from "@mui/icons-material/Visibility"
import {formatRelativeDate} from "../../utils/date"
import {useState, type MouseEvent} from "react"
import useAuth from "../../auth/useAuth"
import {useNavigate} from "react-router-dom"
import {showGlobalConfirm, showToast} from "../../api/client"
import {useBookmarkToggleMutation} from "../../queries/bookmark.queries"
import {useIncreasePostViewCountMutation} from "../../queries/post.queries"
import type {PostSummary} from "../../api/post.api"
import { usePostCategory } from "../../hooks/usePostCategory"

type PostItemProps = {
    post?: PostSummary
    isBlogDetail?: boolean
    isLoading?: boolean
}

const PostItem = ({post, isBlogDetail = false, isLoading = false}: PostItemProps) => {
    const navigate = useNavigate()
    const {isLoggedIn, openLogin} = useAuth()

    const bookmarkToggle = useBookmarkToggleMutation({invalidateOnSuccess: false})
    const increaseView = useIncreasePostViewCountMutation()

    const [bookmarked, setBookmarked] = useState(post?.isBookmarked ?? false)
    const [bookmarkCount, setBookmarkCount] = useState(post?.bookmarkCount ?? 0)
    const [viewCount, setViewCount] = useState(post?.viewCount ?? 0)
    const category = usePostCategory(post?.categoryId)

    const stop = (e: MouseEvent<HTMLElement>) => {
        e.preventDefault()
        e.stopPropagation()
    }

    const onOpenPost = (postId: number, postUrl: string) => {
        window.open(postUrl, "_blank", "noopener,noreferrer")

        // ✅ 조회수는 mutation으로 "조용히" 전송
        increaseView.mutate({postId})
        setViewCount((v) => v + 1)
    }

    const onToggleBookmark = async (postId: number) => {
        if (!isLoggedIn) {
            const ok = await showGlobalConfirm({
                title: "로그인",
                message: "로그인이 필요한 기능입니다. 로그인 하시겠습니까?",
                confirmText: "로그인"
            })
            if (!ok) {
                return
            }
            openLogin()
            return
        }
        if (bookmarkToggle.isPending) return

        const next = !bookmarked

        // ✅ optimistic update
        setBookmarked(next)
        setBookmarkCount((c) => Math.max(0, c + (next ? 1 : -1)))

        try {
            const res = await bookmarkToggle.mutateAsync({postId})
            const finalBookmarked = !!res?.bookmarked

            setBookmarked(finalBookmarked)

            if (finalBookmarked) showToast("북마크 되었습니다.")
            else showToast("북마크 해제하였습니다.")
        } catch {
            // rollback
            setBookmarked(!next)
            setBookmarkCount((c) => Math.max(0, c + (next ? -1 : 1)))
        }
    }

    // ✅ 스켈레톤 (조회 로딩일 때)
    if (isLoading) {
        return (
            <article className={`${styles.card} ${styles.skeletonCard}`} aria-busy="true">
                <div className={styles.postInfo}>
                    <div className={styles.top}>
                        <div className={styles.skeletonTop}>
                            <div className={`${styles.skeleton} ${styles.skeletonCircleSmall}`}/>
                            <div className={`${styles.skeleton} ${styles.skeletonSourceLine}`}/>
                        </div>
                        <div className={`${styles.skeleton} ${styles.skeletonCategory}`}/>
                    </div>

                    <div className={styles.bottom}>
                        <div className={styles.contents}>
                            <div className={`${styles.title} ${styles.skeleton} ${styles.skeletonLine}`}/>
                            <div className={`${styles.summary} ${styles.skeleton} ${styles.skeletonLine}`}/>
                            <div className={`${styles.summary} ${styles.skeleton} ${styles.skeletonLineLong}`}/>
                        </div>

                        <div className={styles.metaRow}>
                            <div className={`${styles.skeleton} ${styles.skeletonMeta}`}/>
                            <div className={`${styles.skeleton} ${styles.skeletonMeta}`}/>
                            <div className={`${styles.skeleton} ${styles.skeletonMeta}`}/>
                        </div>
                    </div>
                </div>

                <div className={styles.thumbnailWrap}>
                    <div className={`${styles.thumbnailImage} ${styles.skeleton} ${styles.skeletonThumb}`}/>
                </div>

                <div className={`${styles.bookmarkButton} ${styles.skeleton} ${styles.skeletonCircle}`}/>
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
            <div className={styles.postInfo}>
                <div className={styles.top}>
                    <div
                        className={styles.sourceLink}
                        onClick={(e) => {
                            stop(e)
                            navigate(`/${post.techBlogId}`)
                        }}
                    >
                        <img
                            src={post.techBlogIcon || ""}
                            alt={post.techBlogTitle || ""}
                            className={styles.sourceIcon}
                        />
                        <span className={styles.sourceName}>{post.techBlogTitle || ""}</span>
                    </div>
                    { category.id != 999 &&
                        <div className={styles.category}>{category.title}</div>
                    }
                </div>

                <div className={styles.bottom}>
                    <div className={styles.contents}>
                        <div className={styles.title}>{post.title}</div>
                        <div className={styles.summary}>{post.description}</div>
                    </div>

                    <div className={styles.metaRow}>
                        <time className={styles.date}>{formatRelativeDate(post.publishedAt)}</time>

                        <span className={styles.dot}>·</span>

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

            { !post.thumbnail.startsWith("https://i.imgur.com/") &&
                <div className={styles.thumbnailWrap}>
                    <img
                        src={post.thumbnail}
                        alt="thumbnail"
                        className={styles.thumbnailImage}
                    />
                </div>
            }

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
                    <BookmarkIcon sx={{fontSize: 16, color: "#0E4BBC", fontWeight: 800}}/>
                ) : (
                    <BookmarkBorderIcon sx={{fontSize: 16, color: "#0E4BBC", fontWeight: 800}}/>
                )}
            </button>
        </article>
    )
}

export default PostItem
