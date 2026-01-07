package server.member.lookup

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class MemberSubscriptionLookup(
    private val jdbc: NamedParameterJdbcTemplate
) {
    val subscribingBlogIdsByMemberId: Map<Long, List<Long>> by lazy {
        jdbc.query(
            """
            SELECT
                tbs.member_id,
                tbs.tech_blog_id
            FROM tech_blog_subscription tbs
            JOIN tech_blog tb
              ON tb.id = tbs.tech_blog_id
            WHERE tbs.notification_enabled = 1
            """.trimIndent()
        ) { rs, _ ->
            rs.getLong("member_id") to rs.getLong("tech_blog_id")
        }.groupBy(
            { it.first },
            { it.second }
        )
    }
}