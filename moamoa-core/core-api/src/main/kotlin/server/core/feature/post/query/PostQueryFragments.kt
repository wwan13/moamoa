package server.core.feature.post.query

import server.core.feature.techblog.application.TechBlogData
import java.sql.ResultSet
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

fun mapToPostSummary(rs: ResultSet): PostSummary = PostSummary(
    id = rs.getLong("post_id"),
    key = rs.getString("post_key") ?: "",
    title = rs.getString("post_title") ?: "",
    description = rs.getString("post_description") ?: "",
    thumbnail = rs.getString("post_thumbnail") ?: "",
    url = rs.getString("post_url") ?: "",
    publishedAt = rs.getObject("published_at", LocalDateTime::class.java) ?: LocalDateTime.MIN,
    viewCount = rs.getLong("post_view_count"),
    bookmarkCount = rs.getLong("post_bookmark_count"),
    isBookmarked = (rs.getInt("is_bookmarked")) == 1,
    techBlog = TechBlogData(
        id = rs.getLong("tech_blog_id"),
        title = rs.getString("tech_blog_title") ?: "",
        key = rs.getString("tech_blog_key") ?: "",
        blogUrl = rs.getString("tech_blog_url") ?: "",
        icon = rs.getString("tech_blog_icon") ?: "",
        subscriptionCount = rs.getLong("tech_blog_subscription_count"),
    )
)
