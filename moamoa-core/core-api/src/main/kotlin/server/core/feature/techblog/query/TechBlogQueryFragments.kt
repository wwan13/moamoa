package server.core.feature.techblog.query

import java.sql.ResultSet

const val TECH_BLOG_QUERY_BASE_SELECT = """
    SELECT
        t.id                 AS tech_blog_id,
        t.title              AS tech_blog_title,
        t.icon               AS tech_blog_icon,
        t.blog_url           AS tech_blog_url,
        t.tech_blog_key      AS tech_blog_key,
        t.subscription_count AS tech_blog_subscription_count,
        COALESCE(pc.post_count, 0) AS tech_blog_post_count
"""

fun mapToTechBlogSummary(rs: ResultSet): TechBlogSummary = TechBlogSummary(
    id = rs.getLong("tech_blog_id"),
    title = rs.getString("tech_blog_title") ?: "",
    icon = rs.getString("tech_blog_icon") ?: "",
    blogUrl = rs.getString("tech_blog_url") ?: "",
    key = rs.getString("tech_blog_key") ?: "",
    subscriptionCount = rs.getLong("tech_blog_subscription_count"),
    postCount = rs.getLong("tech_blog_post_count"),
    subscribed = false,
    notificationEnabled = false,
)
