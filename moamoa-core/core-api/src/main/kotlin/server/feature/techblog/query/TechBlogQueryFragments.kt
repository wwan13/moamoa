package server.feature.techblog.query

import io.r2dbc.spi.Row

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

fun mapToTechBlogSummary(row: Row): TechBlogSummary =
    TechBlogSummary(
        id = row.get("tech_blog_id", Long::class.java) ?: 0L,
        title = row.get("tech_blog_title", String::class.java).orEmpty(),
        icon = row.get("tech_blog_icon", String::class.java).orEmpty(),
        blogUrl = row.get("tech_blog_url", String::class.java).orEmpty(),
        key = row.get("tech_blog_key", String::class.java).orEmpty(),
        subscriptionCount = row.get("tech_blog_subscription_count", Long::class.java) ?: 0L,
        postCount = row.get("tech_blog_post_count", Long::class.java) ?: 0L,
        subscribed =(row.get("is_subscribed", Int::class.java) ?: 0) == 1,
        notificationEnabled = (row.get("notification_enabled", Int::class.java) ?: 0) == 1
    )