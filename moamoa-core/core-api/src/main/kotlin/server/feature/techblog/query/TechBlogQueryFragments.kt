package server.feature.techblog.query

import io.r2dbc.spi.Row
import server.infra.db.getInt01
import server.infra.db.getOrDefault

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

fun mapToTechBlogSummary(row: Row): TechBlogSummary = TechBlogSummary(
    id = row.getOrDefault("tech_blog_id", 0L),
    title = row.getOrDefault("tech_blog_title", ""),
    icon = row.getOrDefault("tech_blog_icon", ""),
    blogUrl = row.getOrDefault("tech_blog_url", ""),
    key = row.getOrDefault("tech_blog_key", ""),
    subscriptionCount = row.getOrDefault("tech_blog_subscription_count", 0L),
    postCount = row.getOrDefault("tech_blog_post_count", 0L),
    subscribed = row.getInt01("is_subscribed"),
    notificationEnabled = row.getInt01("notification_enabled"),
)