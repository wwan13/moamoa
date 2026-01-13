package server.feature.post.query

import io.r2dbc.spi.Row
import server.feature.techblog.command.application.TechBlogData
import server.infra.db.getInt01
import server.infra.db.getOrDefault
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
    id = row.getOrDefault("post_id", 0L),
    key = row.getOrDefault("post_key", ""),
    title = row.getOrDefault("post_title", ""),
    description = row.getOrDefault("post_description", ""),
    thumbnail = row.getOrDefault("post_thumbnail", ""),
    url = row.getOrDefault("post_url", ""),
    publishedAt = row.getOrDefault("published_at", LocalDateTime.MIN),
    viewCount = row.getOrDefault("post_view_count", 0L),
    bookmarkCount = row.getOrDefault("post_bookmark_count", 0L),
    isBookmarked = row.getInt01("is_bookmarked"),
    techBlog = TechBlogData(
        id = row.getOrDefault("tech_blog_id", 0L),
        title = row.getOrDefault("tech_blog_title", ""),
        key = row.getOrDefault("tech_blog_key", ""),
        blogUrl = row.getOrDefault("tech_blog_url", ""),
        icon = row.getOrDefault("tech_blog_icon", ""),
        subscriptionCount = row.getOrDefault("tech_blog_subscription_count", 0L),
    )
)