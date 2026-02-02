package server.techblogs.hyperconnect

import kotlinx.coroutines.flow.Flow
import org.jsoup.nodes.Element
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
import java.util.Locale

@Component
internal class HyperconnectSource : TechBlogSource {

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        val seenKeys = HashSet<String>(2048)

        return fetchWithPaging(size, ::buildListUrl, timeoutMs = TIMEOUT_MS) { doc ->
            val linkEls = doc.select("article a[href], .post-list a[href], .posts a[href], main a[href]")
            if (linkEls.isEmpty()) throw PagingFinishedException()

            val parsed = linkEls.mapNotNull { linkEl ->
                val url = linkEl.absUrl("href").trim()
                if (url.isBlank() || !url.startsWith(BASE_URL)) return@mapNotNull null
                if (isNonPostUrl(url)) return@mapNotNull null

                val key = extractKeyFromUrl(url) ?: return@mapNotNull null
                val title = requireField(url, "title", extractTitle(linkEl))
                val description = extractDescription(linkEl)
                val publishedAt = extractPublishedAt(linkEl)

                TechBlogPost(
                    key = key,
                    title = title,
                    description = description,
                    tags = extractTags(linkEl),
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
        if (page == 1) BASE_URL else "$BASE_URL/page/$page/"

    private fun extractKeyFromUrl(url: String): String? {
        val path = runCatching { URI(url).path }.getOrNull()?.trim('/')?.trim().orEmpty()
        if (path.isBlank()) return null
        if (path.startsWith("page/")) return null
        if (path.startsWith("tag/") || path.startsWith("tags/")) return null
        if (path.startsWith("category/") || path.startsWith("categories/")) return null
        if (path == "about" || path == "archive") return null
        if (path.endsWith(".xml")) return null
        return path
    }

    private fun isNonPostUrl(url: String): Boolean {
        val path = runCatching { URI(url).path }.getOrNull()?.trim('/').orEmpty()
        if (path.isBlank()) return true
        if (path.startsWith("page/")) return true
        if (path.startsWith("tag/") || path.startsWith("tags/")) return true
        if (path.startsWith("category/") || path.startsWith("categories/")) return true
        if (path == "about" || path == "archive") return true
        if (path.endsWith(".xml")) return true
        return false
    }

    private fun extractTitle(linkEl: Element): String {
        return linkEl.selectFirst("h1, h2, h3")?.text()?.trim()
            ?: linkEl.text().trim()
    }

    private fun extractDescription(linkEl: Element): String {
        val container = linkEl.closest("article, li, div, section") ?: linkEl
        val candidate = container.select("p")
            .firstOrNull { it.selectFirst("a") == null }
            ?.text()
            ?.trim()
        return candidate.orEmpty()
    }

    private fun extractTags(linkEl: Element): List<String> {
        val container = linkEl.closest("article, li, div, section") ?: linkEl
        return container.select("a[rel=tag], .tag, .tags a, span")
            .mapNotNull { it.text().normalizeTagTitle().takeIf { tag -> tag.isNotBlank() } }
            .distinct()
    }

    private fun extractPublishedAt(linkEl: Element): LocalDateTime {
        val container = linkEl.closest("article, li, div, section") ?: linkEl
        val timeAttr = container.selectFirst("time[datetime]")?.attr("datetime")?.trim()
        if (!timeAttr.isNullOrBlank()) return parsePublishedAt(timeAttr)

        val candidates = container.select("time, .post-meta, .meta, span, div")
            .asSequence()
            .map { it.text().trim() }
            .filter { it.isNotBlank() }

        for (text in candidates) {
            val match = DATE_REGEX.find(text)?.value
            if (!match.isNullOrBlank()) return parsePublishedAt(match)
        }

        return LocalDateTime.MIN
    }

    private fun parsePublishedAt(raw: String): LocalDateTime {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return LocalDateTime.MIN

        runCatching { return LocalDate.parse(trimmed, DateTimeFormatter.ISO_DATE).atStartOfDay() }
        DATE_FORMATTERS.forEach { formatter ->
            runCatching { return LocalDate.parse(trimmed, formatter).atStartOfDay() }
        }
        return LocalDateTime.MIN
    }

    private fun requireField(url: String, field: String, value: String?): String {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isBlank()) {
            throw IllegalStateException("blogKey=$BLOG_KEY url=$url field=$field")
        }
        return trimmed
    }

    companion object {
        private const val BLOG_KEY = "hyperconnect"
        private const val BASE_URL = "https://hyperconnect.github.io"
        private const val TIMEOUT_MS = 10_000
        private const val DEFAULT_THUMBNAIL = "https://i.imgur.com/wYfXo58.png"

        private val DATE_REGEX = Regex("\\d{4}[./-]\\d{1,2}[./-]\\d{1,2}|[A-Za-z]{3,}\\s+\\d{1,2},\\s+\\d{4}")

        private val DATE_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("yyyy.M.d"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)
        )
    }
}
