package server.source.jsoup

import kotlinx.coroutines.flow.Flow
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component
import server.source.common.PagingFinishedException
import server.source.jsoup.util.fetchWithPaging
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import java.net.URI
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
internal class KakaoMobilitySource : TechBlogSource {

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        var fetched = false
        return fetchWithPaging(targetCount = size, buildUrl = { FEED_URL }, timeoutMs = TIMEOUT_MS) { doc ->
            if (fetched) throw PagingFinishedException()
            fetched = true

            val items = parseItems(doc)
            if (items.isEmpty()) throw PagingFinishedException()
            items
        }
    }

    private fun parseItems(doc: Document): List<TechBlogPost> {
        return doc.select("channel > item").mapNotNull(::toPost)
    }

    private fun toPost(item: Element): TechBlogPost? {
        val url = item.selectFirst("link")?.text()
            ?.trim()
            .orEmpty()

        val resolvedUrl = requireField(url, "url", null)

        val title = requireField(item.selectFirst("title")?.text(), "title", resolvedUrl)

        val keyRaw = item.selectFirst("guid")?.text()?.trim().orEmpty().ifBlank { resolvedUrl }
        val key = requireField(normalizeKey(keyRaw), "key", resolvedUrl)

        val description = item.selectFirst("description")
            ?.text()
            ?.let { Jsoup.parse(it).text().trim() }
            .orEmpty()

        val thumbnail = item.selectFirst("enclosure")
            ?.attr("url")
            ?.trim()
            .orEmpty()
            .ifBlank { DEFAULT_THUMBNAIL }

        val publishedAt = parsePublishedAt(item.selectFirst("pubDate")?.text())

        return TechBlogPost(
            key = key,
            title = title,
            description = description,
            tags = emptyList(),
            thumbnail = thumbnail,
            publishedAt = publishedAt,
            url = resolvedUrl
        )
    }

    private fun parsePublishedAt(raw: String?): LocalDateTime {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return LocalDateTime.MIN

        return runCatching {
            ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime()
        }.getOrElse { LocalDateTime.MIN }
    }

    private fun normalizeKey(raw: String): String {
        val cleaned = raw.trim().trimEnd('/')
        if (cleaned.isBlank()) return cleaned

        val uri = runCatching { URI(cleaned) }.getOrNull()
        val path = uri?.path?.trimEnd('/')?.trim()?.trim('/')
        return if (!path.isNullOrBlank()) path else cleaned
    }

    private fun requireField(value: String?, field: String, url: String?): String {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isBlank()) {
            val urlValue = url?.takeIf { it.isNotBlank() } ?: "unknown"
            throw IllegalStateException("blogKey=$BLOG_KEY, url=$urlValue, field=$field")
        }
        return trimmed
    }

    companion object {
        private const val BLOG_KEY = "kakaoMobility"
        private const val FEED_URL = "https://developers.kakaomobility.com/techblogs.xml"
        private const val TIMEOUT_MS = 10_000
        private const val DEFAULT_THUMBNAIL = ""
    }
}
