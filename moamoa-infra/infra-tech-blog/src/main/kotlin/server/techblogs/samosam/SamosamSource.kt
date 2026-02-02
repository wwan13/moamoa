package server.techblogs.samosam

import kotlinx.coroutines.flow.Flow
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
internal class SamosamSource : TechBlogSource {

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        val seenKeys = HashSet<String>(2048)

        return fetchWithPaging(
            targetCount = size,
            buildUrl = ::buildListUrl,
            timeoutMs = TIMEOUT_MS
        ) { doc ->
            val items = doc.select("article.post-card")
            if (items.isEmpty()) throw PagingFinishedException()

            val parsed = items.mapNotNull { item ->
                val url = item.selectFirst("a.post-card-content-link")
                    ?.absUrl("href")
                    ?.trim()
                    ?: return@mapNotNull null

                if (url.isBlank()) return@mapNotNull null

                val key = extractKey(url)
                if (key.isBlank()) return@mapNotNull null

                val title = item.selectFirst("h2.post-card-title")
                    ?.text()
                    ?.trim()
                    ?: throw IllegalStateException("blogKey=$BLOG_KEY url=$url field=title")

                val description = item.selectFirst(".post-card-excerpt")
                    ?.text()
                    ?.trim()
                    .orEmpty()

                val publishedAt = extractPublishedAt(item, url)

                val thumbnail = item.selectFirst("img.post-card-image")
                    ?.absUrl("src")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("blogKey=$BLOG_KEY url=$url field=thumbnail")

                val tags = item.select(".post-card-tags .post-card-primary-tag")
                    .mapNotNull { it.text()?.normalizeTagTitle()?.ifBlank { null } }
                    .distinct()

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

    private fun buildListUrl(page: Int): String {
        return if (page == 1) {
            "$BASE_URL/"
        } else {
            "$BASE_URL/page/$page/"
        }
    }

    private fun extractPublishedAt(item: org.jsoup.nodes.Element, url: String): LocalDateTime {
        val raw = item.selectFirst("time.post-card-meta-date")
            ?.attr("datetime")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: item.selectFirst("time.post-card-meta-date")
                ?.text()
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        if (raw.isNullOrBlank()) return LocalDateTime.MIN

        return parseDateTime(raw)
            ?: throw IllegalStateException("blogKey=$BLOG_KEY url=$url field=publishedAt")
    }

    private fun parseDateTime(raw: String): LocalDateTime? {
        val text = raw.trim()
        if (text.isBlank()) return null

        runCatching { return LocalDate.parse(text, DATE_FORMATTER_DOT).atStartOfDay() }
        runCatching { return LocalDate.parse(text, DATE_FORMATTER_DOT_FLEX).atStartOfDay() }
        return null
    }

    private fun extractKey(url: String): String {
        val key = URI(url).path.trimEnd('/').substringAfterLast('/').trim()
        if (key.isBlank()) throw IllegalStateException("blogKey=$BLOG_KEY url=$url field=key")
        return key
    }

    companion object {
        private const val BLOG_KEY = "samosam"
        private const val BASE_URL = "https://blog.3o3.co.kr/tag/tech"
        private const val TIMEOUT_MS = 10_000
        private val DATE_FORMATTER_DOT = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        private val DATE_FORMATTER_DOT_FLEX = DateTimeFormatter.ofPattern("yyyy.M.d")
    }
}
