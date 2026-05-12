package server.source.jsoup

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
import java.time.LocalDate
import java.time.LocalDateTime
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

    internal fun parseItems(doc: Document): List<TechBlogPost> {
        return parseDataAsset(doc).ifEmpty {
            parseHtmlCards(doc)
        }
    }

    internal fun parseDataAsset(doc: Document, fetchAsset: (String) -> String = ::fetchBody): List<TechBlogPost> {
        val assetUrl = doc.selectFirst("link[rel=modulepreload][href*=techblogs.data.]")
            ?.absUrl("href")
            ?.trim()
            .orEmpty()

        if (assetUrl.isBlank()) return emptyList()

        val rawJson = runCatching {
            extractJsonArray(fetchAsset(assetUrl))
        }.getOrDefault("")
        if (rawJson.isBlank()) return emptyList()

        return runCatching {
            objectMapper.readTree(rawJson)
                .mapNotNull(::toPost)
                .distinctBy { it.url }
        }.getOrDefault(emptyList())
    }

    private fun parseHtmlCards(doc: Document): List<TechBlogPost> {
        val cards = doc.select("a.new-content-card[href], a.blog-card[href]")
        return cards.mapNotNull(::toPost).distinctBy { it.url }
    }

    private fun toPost(node: JsonNode): TechBlogPost? {
        val url = resolveUrl(node.path("link").asText())
        val title = requireField(node.path("title").asText(), "title", url)
        val publishedAt = parsePublishedAt(node.path("date").asText())

        return TechBlogPost(
            key = requireField(normalizeKey(url), "key", url),
            title = title,
            description = node.path("description").asText().trim(),
            tags = listOfNotNull(node.path("category").asText().trim().takeIf { it.isNotBlank() }),
            thumbnail = node.path("image").asText().trim(),
            publishedAt = publishedAt,
            url = url
        )
    }

    private fun toPost(card: Element): TechBlogPost? {
        val url = resolveUrl(card.attr("href"))
        val title = card.selectFirst(".new-content-card-title, .blog-card__title")
            ?.normalizedText()
            ?: return null
        val description = card.selectFirst(".blog-card__description")?.normalizedText().orEmpty()
        val thumbnail = card.selectFirst("img[src]")?.absUrl("src").orEmpty()
        val date = card.selectFirst(".new-content-card-time")
            ?.normalizedText()
            ?: card.selectFirst(".blog-card__meta")
                ?.normalizedText()
                ?.substringBefore("|")
                ?.trim()
                .orEmpty()

        return TechBlogPost(
            key = requireField(normalizeKey(url), "key", url),
            title = title,
            description = description,
            tags = emptyList(),
            thumbnail = thumbnail,
            publishedAt = parsePublishedAt(date),
            url = url
        )
    }

    private fun parsePublishedAt(raw: String): LocalDateTime {
        val value = raw.trim()
        if (value.isBlank()) return LocalDateTime.MIN

        return runCatching {
            LocalDate.parse(value, dateFormatter).atStartOfDay()
        }.getOrElse { LocalDateTime.MIN }
    }

    private fun resolveUrl(raw: String): String =
        requireField(URI(BASE_URL).resolve(raw.trim()).toString(), "url", null)

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

    private fun extractJsonArray(script: String): String =
        Regex("""JSON\.parse\(`(.*)`\)""", RegexOption.DOT_MATCHES_ALL)
            .find(script)
            ?.groupValues
            ?.get(1)
            ?.replace("\\`", "`")
            ?.replace("\\$", "$")
            .orEmpty()

    private fun fetchBody(url: String): String =
        Jsoup.connect(url)
            .timeout(TIMEOUT_MS)
            .userAgent(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36"
            )
            .ignoreContentType(true)
            .execute()
            .body()

    private fun Element.normalizedText(): String? =
        text().trim().takeIf { it.isNotBlank() }

    companion object {
        private const val BLOG_KEY = "kakaoMobility"
        private const val BASE_URL = "https://developers.kakaomobility.com"
        private const val FEED_URL = "$BASE_URL/techblogs/"
        private const val TIMEOUT_MS = 10_000
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        private val objectMapper = ObjectMapper()
    }
}
