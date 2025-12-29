import styles from "./PostItem.module.css"
import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder"
import BookmarkIcon from "@mui/icons-material/Bookmark"
import VisibilityIcon from "@mui/icons-material/Visibility"
import {postsViewCountApi} from "../../api/post.api.js";
import {formatRelativeDate} from "../../utils/date.js";

export default function PostItem({post}) {
    const onOpenPost = (postId, postUrl) => {
        window.open(postUrl, "_blank", "noopener,noreferrer")
        postsViewCountApi(postId)
    }

    return (
        <article
            className={styles.card}
            onClick={() => onOpenPost(post.id, post.url)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === "Enter" && onOpenPost(post.id)}
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
                    onClick={() => onToggleBookmark(post.id)}
                    aria-label={post.bookmarked ? "북마크 해제" : "북마크"}
                >
                    {post.bookmarked ? (
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

                {/* ✅ 메타 정보 */}
                <div className={styles.metaRow}>
                    <div className={styles.source}>
                        <a
                            href={post.sourceUrl}
                            rel="noopener noreferrer"
                            className={styles.sourceLink}
                        >
                            <img
                                src={post.techBlog?.icon || ""}
                                alt={post.techBlog?.title || ""}
                                className={styles.sourceIcon}
                            />
                            <span className={styles.sourceName}>{post.techBlog?.title || ""}</span>
                        </a>

                        <span className={styles.dot}>·</span>
                        <time className={styles.date}>{formatRelativeDate(post.publishedAt)}</time>
                    </div>

                    <div className={styles.stats}>
                        <span className={styles.stat}>
                            <BookmarkBorderIcon fontSize="inherit"/>
                            {post.bookmarkCount}
                        </span>
                        <span className={styles.dot}>·</span>
                        <span className={styles.stat}>
                            <VisibilityIcon fontSize="inherit"/>
                            {post.viewCount}
                        </span>
                    </div>
                </div>
            </div>
        </article>
    )
}