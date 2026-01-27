package server.techblogs.socar

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
internal class SocarSource : TechBlogSource {

    private val baseUrl = "https://tech.socarcorp.kr"
    private val timeoutMs = 10_000

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        val seenKeys = HashSet<String>(2048)

        return fetchWithPaging(size, ::buildListUrl, timeoutMs = timeoutMs) { doc ->
            val items = doc.select("article.post-preview")
            if (items.isEmpty()) throw PagingFinishedException()

            val parsed = items.mapNotNull { item ->
                val linkEl = item.selectFirst("a[href]") ?: return@mapNotNull null
                val url = linkEl.absUrl("href").trim()
                if (url.isBlank()) return@mapNotNull null

                val key = extractKeyFromUrl(url)
                if (key.isBlank()) return@mapNotNull null

                val title = item.selectFirst("h2.post-title")?.text()?.trim()
                    ?: return@mapNotNull null

                val description = item.selectFirst("h3.post-subtitle")
                    ?.text()
                    ?.trim()
                    .orEmpty()

                val publishedAt = item.selectFirst("span.date")
                    ?.text()
                    ?.trim()
                    ?.let(::parseDateTime)
                    ?: LocalDateTime.MIN

                val tags = item.select("span.tag a")
                    .map { it.text().normalizeTagTitle() }
                    .filter { it.isNotBlank() }
                    .distinct()

                TechBlogPost(
                    key = key,
                    title = title,
                    description = description,
                    tags = tags,
                    thumbnail = DEFAULT_THUMBNAIL,
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
        if (page == 1) "$baseUrl/posts/" else "$baseUrl/posts/page$page/"

    private fun extractKeyFromUrl(url: String): String {
        val cleaned = url.substringBefore('?').substringBefore('#')
        val path = runCatching { URI(cleaned).path }.getOrDefault(cleaned)
        return path
            .trimEnd('/')
            .removePrefix("/")
            .removeSuffix(".html")
            .trim()
    }

    private fun parseDateTime(raw: String): LocalDateTime =
        LocalDate.parse(raw.trim(), DATE_FORMATTER).atStartOfDay()

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private const val DEFAULT_THUMBNAIL = "https://i.imgur.com/4cjgXEL.png"
    }
}
