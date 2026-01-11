package server.feature.post.query

import io.r2dbc.spi.Row
import server.feature.techblog.command.application.TechBlogData
import java.time.LocalDateTime

const val POST_QUERY_BASE_SELECT = """
    SELECT
        p.id               AS post_id,
        p.post_key         AS post_key,
        p.title            AS post_title,
        p.description      AS post_description,
        p.thumbnail        AS post_thumbnail,
        p.url              AS post_url,
        p.published_at     AS published_at,
        p.view_count       AS post_view_count,
        p.bookmark_count   AS post_bookmark_count,
        
        t.id               AS tech_blog_id,
        t.title            AS tech_blog_title,
        t.tech_blog_key    AS tech_blog_key,
        t.blog_url         AS tech_blog_url,
        t.icon             AS tech_blog_icon,
        t.subscription_count AS tech_blog_subscription_count
"""

fun mapToPostSummary(row: Row): PostSummary = PostSummary(
    id = row.get("post_id", Long::class.java) ?: 0L,
    key = row.get("post_key", String::class.java).orEmpty(),
    title = row.get("post_title", String::class.java).orEmpty(),
    description = row.get("post_description", String::class.java).orEmpty(),
    thumbnail = row.get("post_thumbnail", String::class.java).orEmpty(),
    url = row.get("post_url", String::class.java).orEmpty(),
    publishedAt = row.get("published_at", LocalDateTime::class.java) ?: LocalDateTime.MIN,
    viewCount = row.get("post_view_count", Long::class.java) ?: 0L,
    bookmarkCount = row.get("post_bookmark_count", Long::class.java) ?: 0L,
    isBookmarked = (row.get("is_bookmarked", Int::class.java) == 1),
    techBlog = TechBlogData(
        id = row.get("tech_blog_id", Long::class.java) ?: 0L,
        title = row.get("tech_blog_title", String::class.java).orEmpty(),
        key = row.get("tech_blog_key", String::class.java).orEmpty(),
        blogUrl = row.get("tech_blog_url", String::class.java).orEmpty(),
        icon = row.get("tech_blog_icon", String::class.java).orEmpty(),
        subscriptionCount = row.get("tech_blog_subscription_count", Long::class.java) ?: 0L
    )
)
