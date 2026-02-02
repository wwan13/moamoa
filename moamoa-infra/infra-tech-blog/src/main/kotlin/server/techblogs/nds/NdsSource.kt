package server.techblogs.nds

import org.springframework.stereotype.Component
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import server.utill.PagingFinishedException
import server.utill.fetchWithPaging
import server.utill.normalizeTagTitle
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
internal class NdsSource : TechBlogSource {

    override suspend fun getPosts(size: Int?) = run {
        val seenKeys = HashSet<String>(2048)

        fetchWithPaging(
            targetCount = size,
            buildUrl = ::buildListUrl,
            timeoutMs = TIMEOUT_MS
        ) { doc ->
            val articles = doc.select("#blog-entries article.blog-entry")
            if (articles.isEmpty()) throw PagingFinishedException()

            val parsed = articles.map { article ->
                val linkEl = article.selectFirst("h2.blog-entry-title a[rel=bookmark][href]")
                    ?: throw IllegalStateException("blogKey=$BLOG_KEY url=<unknown> field=url")

                val url = requireField(linkEl.absUrl("href"), "url", null)
                val title = requireField(linkEl.text(), "title", url)
                val key = requireField(extractKey(url), "key", url)

                val description = article.selectFirst(".blog-entry-summary")
                    ?.text()
                    ?.trim()
                    ?.takeIf { it.isNotBlank() && it != ELLIPSIS }
                    .orEmpty()

                val tags = article.select(".blog-entry-category a[href]")
                    .mapNotNull { it.text().trim().normalizeTagTitle().ifBlank { null } }
                    .distinct()

                val thumbnail = article.selectFirst(".thumbnail img[src]")
                    ?.absUrl("src")
                    ?.takeIf { it.isNotBlank() }
                    ?: DEFAULT_THUMBNAIL

                val publishedAt = article.selectFirst(".blog-entry-date")
                    ?.text()
                    ?.trim()
                    ?.let(::parsePublishedAt)
                    ?: LocalDateTime.MIN

                TechBlogPost(
                    key = key,
                    title = title,
                    description = description,
                    tags = tags,
                    thumbnail = thumbnail,
                    publishedAt = publishedAt,
                    url = url
                )
            }

            val unique = parsed.filter { seenKeys.add(it.key) }
            if (unique.isEmpty()) throw PagingFinishedException()

            unique
        }
    }

    private fun buildListUrl(page: Int): String =
        if (page == 1) "$BASE_URL/post/" else "$BASE_URL/post/page/$page/"

    private fun requireField(value: String?, field: String, url: String?): String {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isBlank()) {
            val urlValue = url?.takeIf { it.isNotBlank() } ?: "<unknown>"
            throw IllegalStateException("blogKey=$BLOG_KEY url=$urlValue field=$field")
        }
        return trimmed
    }

    private fun extractKey(url: String): String {
        val path = URI(url).path.trimEnd('/')
        return path.substringAfterLast('/').trim()
    }

    private fun parsePublishedAt(raw: String): LocalDateTime? =
        runCatching { LocalDate.parse(raw, DATE_FORMATTER).atStartOfDay() }.getOrNull()

    companion object {
        private const val BLOG_KEY = "nds"
        private const val BASE_URL = "https://tech.cloud.nongshim.co.kr"
        private const val DEFAULT_THUMBNAIL = "https://i.imgur.com/wzCJPPY.png"
        private const val ELLIPSIS = "…"
        private const val TIMEOUT_MS = 10_000
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
