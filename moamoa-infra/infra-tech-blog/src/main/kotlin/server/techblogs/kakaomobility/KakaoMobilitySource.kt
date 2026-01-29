package server.techblogs.kakaomobility

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import server.utill.PagingFinishedException
import server.utill.fetchWithPaging
import server.utill.handlePagingFinished
import server.utill.validateIsPagingFinished
import java.net.URI
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
internal class KakaoMobilitySource(
    private val webClient: WebClient
) : TechBlogSource {

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        return fetchWithPaging(pageSize = PAGE_SIZE, targetCount = size) { limit, page ->
            if (page > 1) throw PagingFinishedException()

            val xml = webClient.get()
                .uri(FEED_URL)
                .retrieve()
                .handlePagingFinished()
                .bodyToMono(String::class.java)
                .awaitSingle()

            val items = parseItems(xml).let { parsed ->
                if (limit >= parsed.size) parsed else parsed.take(limit)
            }
            items.validateIsPagingFinished()
            items
        }
    }

    private fun parseItems(xml: String): List<TechBlogPost> {
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
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
        private const val PAGE_SIZE = 50
        private const val DEFAULT_THUMBNAIL = "https://i.imgur.com/6taXho0.png"
    }
}
