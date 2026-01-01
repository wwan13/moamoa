import styles from "./PostItem.module.css"
import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder"
import BookmarkIcon from "@mui/icons-material/Bookmark"
import VisibilityIcon from "@mui/icons-material/Visibility"
import {postsViewCountApi} from "../../api/post.api.js"
import {formatRelativeDate} from "../../utils/date.js"
import {bookmarkToggleApi} from "../../api/bookmakr.api.js";
import {useEffect, useState} from "react";
import useAuth from "../../auth/AuthContext.jsx";
import {useNavigate} from "react-router-dom";
import {showToast} from "../../api/client.js";

export default function PostItem({post, isBlogDetail}) {
    const navigate = useNavigate()
    const { isLoggedIn, openLogin } = useAuth()
    const [bookmarked, setBookmarked] = useState(post.isBookmarked)
    const [bookmarkCount, setBookmarkCount] = useState(post.bookmarkCount)
    const [viewCount, setViewCount] = useState(post.viewCount)

    useEffect(() => {
        setBookmarkCount(post.bookmarkCount)
        setViewCount(post.viewCount)
    }, [post.id])

    const onOpenPost = (postId, postUrl) => {
        window.open(postUrl, "_blank", "noopener,noreferrer")
        postsViewCountApi(postId)
        setViewCount(viewCount + 1)
    }

    const stop = (e) => {
        e.preventDefault()
        e.stopPropagation()
    }

    const onToggleBookmark = async (postId) => {
        if (!isLoggedIn) {
            openLogin()
            return
        }

        const next = !bookmarked

        setBookmarked(next)
        setBookmarkCount(c => c + (next ? 1 : -1))

        try {
            const res = await bookmarkToggleApi(postId)
            setBookmarked(res.bookmarked)

            if (res.bookmarked) {
                showToast("북마크 되었습니다.")
            } else {
                showToast("북마크 해제하였습니다.")
            }
        } catch (e) {
            // rollback
            setBookmarked(!next)
            setBookmarkCount(c => c + (next ? -1 : 1))
        }
    }

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
                    onClick={(e) => {
                        stop(e)
                        onToggleBookmark(post.id)
                    }}
                    aria-label={post.bookmarked ? "북마크 해제" : "북마크"}
                >
                    {bookmarked ? (
                        <BookmarkIcon fontSize="small"/>
                    ) : (
                        <BookmarkBorderIcon fontSize="small"/>
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
                        {!isBlogDetail &&
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
                        }
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