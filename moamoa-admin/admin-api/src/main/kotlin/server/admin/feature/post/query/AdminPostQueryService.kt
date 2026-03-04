package server.admin.feature.post.query

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import server.admin.feature.tag.domain.AdminTag
import server.admin.feature.techblog.application.AdminTechBlogData
import java.sql.ResultSet
import java.time.LocalDateTime

@Service
internal class AdminPostQueryService(
    private val jdbc: NamedParameterJdbcTemplate,
) {

    fun findByConditions(conditions: AdminPostQueryConditions): AdminPostList {
        val size = conditions.size?.takeIf { it > 0 } ?: 20L
        val page = conditions.page?.takeIf { it > 0 } ?: 1L

        if (conditions.techBlogIds != null && conditions.techBlogIds.isEmpty()) {
            return AdminPostList(
                meta = AdminPostListMeta(page = page, size = size, totalCount = 0L, totalPages = 0L),
                posts = emptyList(),
            )
        }

        val filter = buildFilter(conditions)
        val totalCount = fetchTotalCount(filter)
        val totalPages = if (totalCount == 0L) 0L else (totalCount + size - 1L) / size
        val offset = (page - 1L) * size

        val basePosts = fetchBasePosts(filter, size, offset)
        val posts = if (basePosts.isEmpty()) emptyList() else {
            val tagsByPostId = fetchTagsByPostIds(basePosts.map { it.postId })
            basePosts.map { base ->
                AdminPostSummary(
                    postId = base.postId,
                    key = base.key,
                    title = base.title,
                    description = base.description,
                    thumbnail = base.thumbnail,
                    url = base.url,
                    publishedAt = base.publishedAt,
                    categoryId = base.categoryId,
                    techBlog = base.techBlog,
                    tags = tagsByPostId[base.postId].orEmpty(),
                )
            }
        }

        return AdminPostList(
            meta = AdminPostListMeta(page = page, size = size, totalCount = totalCount, totalPages = totalPages),
            posts = posts,
        )
    }

    private fun fetchBasePosts(filter: PostSearchFilter, size: Long, offset: Long): List<BasePost> {
        val sql = """
            SELECT
                p.id            AS post_id,
                p.post_key      AS post_key,
                p.title         AS post_title,
                p.description   AS post_description,
                p.thumbnail     AS post_thumbnail,
                p.url           AS post_url,
                p.published_at  AS published_at,
                p.category_id   AS category_id,
                t.id            AS tech_blog_id,
                t.title         AS tech_blog_title,
                t.icon          AS tech_blog_icon,
                t.blog_url      AS tech_blog_url,
                t.tech_blog_key AS tech_blog_key
            FROM post p
            INNER JOIN tech_blog t ON t.id = p.tech_blog_id
            ${filter.whereClause}
            ORDER BY p.published_at DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = MapSqlParameterSource(filter.params)
            .addValue("limit", size)
            .addValue("offset", offset)

        return jdbc.query(sql, params) { rs, _ -> mapToBasePost(rs) }
    }

    private fun fetchTotalCount(filter: PostSearchFilter): Long {
        val sql = """
            SELECT COUNT(*) AS total_count
            FROM post p
            ${filter.whereClause}
        """.trimIndent()

        return jdbc.queryForObject(sql, MapSqlParameterSource(filter.params), Long::class.java) ?: 0L
    }

    private fun fetchTagsByPostIds(postIds: List<Long>): Map<Long, List<AdminTag>> {
        if (postIds.isEmpty()) return emptyMap()

        val sql = """
            SELECT
                pt.post_id AS post_id,
                tg.id      AS tag_id,
                tg.title   AS tag_title
            FROM post_tag pt
            INNER JOIN tag tg ON tg.id = pt.tag_id
            WHERE pt.post_id IN (:postIds)
            ORDER BY pt.post_id ASC, tg.title ASC
        """.trimIndent()

        val rows = jdbc.query(sql, mapOf("postIds" to postIds)) { rs, _ ->
            rs.getLong("post_id") to AdminTag(
                id = rs.getLong("tag_id"),
                title = rs.getString("tag_title") ?: "",
            )
        }

        return rows.groupBy({ it.first }, { it.second })
    }

    private fun buildFilter(conditions: AdminPostQueryConditions): PostSearchFilter {
        val whereClauses = mutableListOf<String>()
        val params = linkedMapOf<String, Any>()

        conditions.query?.takeIf { it.isNotBlank() }?.let { keyword ->
            whereClauses += """
                (
                    p.title LIKE :keyword
                    OR p.description LIKE :keyword
                    OR EXISTS (
                        SELECT 1
                        FROM post_tag pt
                        INNER JOIN tag tg ON tg.id = pt.tag_id
                        WHERE pt.post_id = p.id
                          AND tg.title LIKE :keyword
                    )
                )
            """.trimIndent()
            params["keyword"] = "%$keyword%"
        }

        conditions.categoryId?.let {
            whereClauses += "p.category_id = :categoryId"
            params["categoryId"] = it
        }

        conditions.techBlogIds?.takeIf { it.isNotEmpty() }?.let {
            whereClauses += "p.tech_blog_id IN (:techBlogIds)"
            params["techBlogIds"] = it
        }

        val where = if (whereClauses.isEmpty()) "" else "WHERE ${whereClauses.joinToString(" AND ")}"
        return PostSearchFilter(whereClause = where, params = params)
    }

    private fun mapToBasePost(rs: ResultSet): BasePost = BasePost(
        postId = rs.getLong("post_id"),
        key = rs.getString("post_key") ?: "",
        title = rs.getString("post_title") ?: "",
        description = rs.getString("post_description") ?: "",
        thumbnail = rs.getString("post_thumbnail") ?: "",
        url = rs.getString("post_url") ?: "",
        publishedAt = rs.getObject("published_at", LocalDateTime::class.java) ?: LocalDateTime.MIN,
        categoryId = rs.getLong("category_id"),
        techBlog = AdminTechBlogData(
            id = rs.getLong("tech_blog_id"),
            title = rs.getString("tech_blog_title") ?: "",
            icon = rs.getString("tech_blog_icon") ?: "",
            blogUrl = rs.getString("tech_blog_url") ?: "",
            key = rs.getString("tech_blog_key") ?: "",
        ),
    )

    private data class BasePost(
        val postId: Long,
        val key: String,
        val title: String,
        val description: String,
        val thumbnail: String,
        val url: String,
        val publishedAt: LocalDateTime,
        val categoryId: Long,
        val techBlog: AdminTechBlogData,
    )

    private data class PostSearchFilter(
        val whereClause: String,
        val params: Map<String, Any>,
    )
}
