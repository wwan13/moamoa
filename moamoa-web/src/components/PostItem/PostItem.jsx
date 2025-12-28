import styles from "./PostItem.module.css"
import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder"
import BookmarkIcon from "@mui/icons-material/Bookmark"
import VisibilityIcon from "@mui/icons-material/Visibility"

export default function PostItem({post}) {
    return (
        <article className={styles.card}>
            <div className={styles.thumbnailWrap}>
                <img
                    src="https://static.toss.im/illusts-content/1205_tech_thumb.png"
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
                    <div className={styles.summary}>{post.summary}</div>
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
                                src="https://avatars.githubusercontent.com/u/25682207?s=200&v=4"
                                alt="toss"
                                className={styles.sourceIcon}
                            />
                            <span className={styles.sourceName}>토스</span>
                        </a>

                        <span className={styles.dot}>·</span>
                        <time className={styles.date}>{post.publishedAt}</time>
                    </div>

                    <div className={styles.stats}>
                        <span className={styles.stat}>
                            <BookmarkBorderIcon fontSize="inherit"/>
                            12
                        </span>
                        <span className={styles.dot}>·</span>
                        <span className={styles.stat}>
                            <VisibilityIcon fontSize="inherit"/>
                            25
                        </span>
                    </div>
                </div>
            </div>
        </article>
    )
}