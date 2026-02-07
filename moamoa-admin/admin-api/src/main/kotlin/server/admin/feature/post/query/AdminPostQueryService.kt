package server.admin.feature.post.query

import io.r2dbc.spi.Row
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import server.admin.feature.tag.domain.AdminTag
import server.admin.feature.techblog.application.AdminTechBlogData
import server.admin.infra.db.databaseclient.getOrDefault
import java.time.LocalDateTime

@Service
internal class AdminPostQueryService(
    private val databaseClient: DatabaseClient,
) {

    suspend fun findByConditions(
        conditions: AdminPostQueryConditions
    ): AdminPostList = coroutineScope {
        val size = conditions.size?.takeIf { it > 0 } ?: 20L
        val page = conditions.page?.takeIf { it > 0 } ?: 1L

        if (conditions.techBlogIds != null && conditions.techBlogIds.isEmpty()) {
            return@coroutineScope AdminPostList(
                meta = AdminPostListMeta(
                    page = page,
                    size = size,
                    totalCount = 0L,
                    totalPages = 0L
                ),
                posts = emptyList()
            )
        }

        val offset = (page - 1L) * size
        val filter = buildFilter(conditions)
        val totalCount = fetchTotalCount(filter)
        val totalPages = if (totalCount == 0L) 0L else (totalCount + size - 1L) / size

        val basePosts = fetchBasePosts(
            filter = filter,
            size = size,
            offset = offset
        )
        val posts = if (basePosts.isEmpty()) {
            emptyList()
        } else {
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
                    tags = tagsByPostId[base.postId].orEmpty()
                )
            }
        }

        AdminPostList(
            meta = AdminPostListMeta(
                page = page,
                size = size,
                totalCount = totalCount,
                totalPages = totalPages
            ),
            posts = posts
        )
    }

    private suspend fun fetchBasePosts(
        filter: PostSearchFilter,
        size: Long,
        offset: Long
    ): List<BasePost> {
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

        var spec = databaseClient.sql(sql)
            .bind("limit", size)
            .bind("offset", offset)

        if (filter.keyword != null) {
            spec = spec.bind("keyword", "%${filter.keyword}%")
        }

        if (filter.categoryId != null) {
            spec = spec.bind("categoryId", filter.categoryId)
        }

        filter.techBlogIdBindings.forEach { (key, value) ->
            spec = spec.bind(key, value)
        }

        return spec
            .map { row, _ -> mapToBasePost(row) }
            .all()
            .asFlow()
            .toList()
    }

    private suspend fun fetchTotalCount(filter: PostSearchFilter): Long {
        val sql = """
            SELECT COUNT(*) AS total_count
            FROM post p
            ${filter.whereClause}
        """.trimIndent()

        var spec = databaseClient.sql(sql)

        if (filter.keyword != null) {
            spec = spec.bind("keyword", "%${filter.keyword}%")
        }

        if (filter.categoryId != null) {
            spec = spec.bind("categoryId", filter.categoryId)
        }

        filter.techBlogIdBindings.forEach { (key, value) ->
            spec = spec.bind(key, value)
        }

        return spec
            .map { row, _ -> row.getOrDefault("total_count", 0L) }
            .one()
            .awaitSingle()
    }

    private fun buildFilter(conditions: AdminPostQueryConditions): PostSearchFilter {
        val whereClauses = mutableListOf<String>()
        val keyword = conditions.query?.takeIf { it.isNotBlank() }

        if (keyword != null) {
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
        }

        if (conditions.categoryId != null) {
            whereClauses += "p.category_id = :categoryId"
        }

        val techBlogIdBindings = mutableMapOf<String, Long>()
        val techBlogIds = conditions.techBlogIds?.toList()
        if (!techBlogIds.isNullOrEmpty()) {
            val placeholders = techBlogIds.mapIndexed { index, id ->
                val key = "techBlogId$index"
                techBlogIdBindings[key] = id
                ":$key"
            }
            whereClauses += "p.tech_blog_id IN (${placeholders.joinToString(", ")})"
        }

        val whereClause = if (whereClauses.isEmpty()) "" else "WHERE ${whereClauses.joinToString(" AND ")}"
        return PostSearchFilter(
            whereClause = whereClause,
            keyword = keyword,
            categoryId = conditions.categoryId,
            techBlogIdBindings = techBlogIdBindings
        )
    }

    private suspend fun fetchTagsByPostIds(
        postIds: List<Long>
    ): Map<Long, List<AdminTag>> {
        if (postIds.isEmpty()) return emptyMap()

        val bindings = mutableMapOf<String, Long>()
        val placeholders = postIds.mapIndexed { index, id ->
            val key = "postId$index"
            bindings[key] = id
            ":$key"
        }

        val sql = """
            SELECT
                pt.post_id AS post_id,
                tg.id      AS tag_id,
                tg.title   AS tag_title
            FROM post_tag pt
            INNER JOIN tag tg ON tg.id = pt.tag_id
            WHERE pt.post_id IN (${placeholders.joinToString(", ")})
            ORDER BY pt.post_id ASC, tg.title ASC
        """.trimIndent()

        var spec = databaseClient.sql(sql)
        bindings.forEach { (key, value) ->
            spec = spec.bind(key, value)
        }

        val rows = spec
            .map { row, _ ->
                val postId = row.getOrDefault("post_id", 0L)
                val tag = AdminTag(
                    id = row.getOrDefault("tag_id", 0L),
                    title = row.getOrDefault("tag_title", "")
                )
                postId to tag
            }
            .all()
            .asFlow()
            .toList()

        return rows.groupBy(
            keySelector = { it.first },
            valueTransform = { it.second }
        )
    }

    private fun mapToBasePost(row: Row): BasePost = BasePost(
        postId = row.getOrDefault("post_id", 0L),
        key = row.getOrDefault("post_key", ""),
        title = row.getOrDefault("post_title", ""),
        description = row.getOrDefault("post_description", ""),
        thumbnail = row.getOrDefault("post_thumbnail", ""),
        url = row.getOrDefault("post_url", ""),
        publishedAt = row.getOrDefault("published_at", LocalDateTime.MIN),
        categoryId = row.getOrDefault("category_id", 0L),
        techBlog = AdminTechBlogData(
            id = row.getOrDefault("tech_blog_id", 0L),
            title = row.getOrDefault("tech_blog_title", ""),
            icon = row.getOrDefault("tech_blog_icon", ""),
            blogUrl = row.getOrDefault("tech_blog_url", ""),
            key = row.getOrDefault("tech_blog_key", "")
        )
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
        val keyword: String?,
        val categoryId: Long?,
        val techBlogIdBindings: Map<String, Long>,
    )
}
