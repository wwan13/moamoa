package server.techblogs.ab180

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import server.utill.jsoup
import server.utill.normalizeTagTitle
import java.net.URI
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
internal class Ab180Source(
    private val webClient: WebClient,
) : TechBlogSource {

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        val items = fetchRssItems()
        val baseFlow = items.asFlow().let { flow ->
            if (size != null) flow.take(size) else flow
        }

        return baseFlow.flatMapMerge(concurrency = 10) { item ->
            flow {
                val detail = fetchDetail(item.url)
                val publishedAt = detail.publishedAt ?: item.publishedAt
                if (publishedAt == null) return@flow

                emit(item.toPost(detail.thumbnail, publishedAt))
            }
        }
    }

    private suspend fun fetchRssItems(): List<RssItem> {
        val xml = webClient.get()
            .uri(RSS_URL)
            .retrieve()
            .bodyToMono(String::class.java)
            .awaitSingle()

        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        val elements = doc.select("item")

        if (elements.isEmpty()) {
            throw IllegalStateException("blogKey=$BLOG_KEY, url=$RSS_URL, field=items")
        }

        val seenKeys = HashSet<String>(elements.size * 2)

        val parsed = elements.mapNotNull { item ->
            val url = extractUrl(item)
            if (!url.contains(STORY_PATH_MARKER)) return@mapNotNull null

            val key = extractKey(url)
            val title = requireField(item.selectFirst("title")?.text(), "title", url)

            val rawDescription = item.selectFirst("content\\:encoded")
                ?.text()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: item.selectFirst("description")?.text()?.trim().orEmpty()

            val description = if (rawDescription.isBlank()) {
                ""
            } else {
                Jsoup.parse(rawDescription).text().trim()
            }

            val tags = item.select("category")
                .mapNotNull { it.text().trim().normalizeTagTitle().takeIf { it.isNotBlank() } }
                .distinct()

            val publishedAt = parsePublishedAt(
                raw = item.selectFirst("pubDate")?.text()?.trim().orEmpty(),
                url = url
            )

            val rssItem = RssItem(
                key = key,
                title = title,
                description = description,
                tags = tags,
                publishedAt = publishedAt,
                url = url
            )

            if (!seenKeys.add(rssItem.key)) return@mapNotNull null
            rssItem
        }

        if (parsed.isEmpty()) {
            throw IllegalStateException("blogKey=$BLOG_KEY, url=$RSS_URL, field=items")
        }

        return parsed
    }

    private fun extractUrl(item: org.jsoup.nodes.Element): String {
        val links = item.select("link")
            .mapNotNull { it.text().trim().ifBlank { null } }

        val url = links.firstOrNull { it.contains(STORY_PATH_MARKER) }
            ?: links.firstOrNull()

        return requireField(url, "url", null)
    }

    private fun extractKey(url: String): String {
        val path = URI(url).path.trimEnd('/')
        val key = path.substringAfter(STORY_PATH_MARKER, "")
        return requireField(key, "key", url)
    }

    private fun parsePublishedAt(raw: String, url: String): LocalDateTime? {
        if (raw.isBlank()) return null
        return runCatching {
            ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime()
        }.getOrNull()
    }

    private suspend fun fetchDetail(url: String): Detail = withContext(Dispatchers.IO) {
        val doc = jsoup(url, timeoutMs = TIMEOUT_MS)
        val thumbnailSelectors = listOf(
            "meta[property=og:image]",
            "meta[property=og:image:secure_url]",
            "meta[name=og:image]",
            "meta[name=twitter:image]",
            "meta[property=twitter:image]",
            "meta[name=twitter:image:src]"
        )

        val thumbnail = thumbnailSelectors.asSequence()
            .mapNotNull { selector -> doc.selectFirst(selector) }
            .mapNotNull { element ->
                val raw = element.attr("content").trim()
                if (raw.isBlank()) null else element.absUrl("content").ifBlank { raw }
            }
            .firstOrNull()

        val publishedAt = extractPublishedAt(doc)

        Detail(
            thumbnail = requireField(thumbnail, "thumbnail", url),
            publishedAt = publishedAt
        )
    }

    private fun extractPublishedAt(doc: org.jsoup.nodes.Document): LocalDateTime? {
        val selectors = listOf(
            "meta[property=article:published_time]",
            "meta[name=article:published_time]",
            "time[datetime]"
        )

        val raw = selectors.asSequence()
            .mapNotNull { selector -> doc.selectFirst(selector) }
            .mapNotNull { element ->
                val value = element.attr("content").ifBlank { element.attr("datetime") }
                value.trim().ifBlank { null }
            }
            .firstOrNull()
            ?: return null

        return parseDetailDateTime(raw)
    }

    private fun parseDetailDateTime(raw: String): LocalDateTime? {
        return runCatching {
            OffsetDateTime.parse(raw).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
        }.getOrNull()
            ?: runCatching {
                ZonedDateTime.parse(raw).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
            }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(raw)
            }.getOrNull()
            ?: runCatching {
                java.time.LocalDate.parse(raw).atStartOfDay()
            }.getOrNull()
    }

    private fun requireField(value: String?, field: String, url: String?): String {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isBlank()) {
            val urlValue = url?.takeIf { it.isNotBlank() } ?: "unknown"
            throw IllegalStateException("blogKey=$BLOG_KEY, url=$urlValue, field=$field")
        }
        return trimmed
    }

    private data class RssItem(
        val key: String,
        val title: String,
        val description: String,
        val tags: List<String>,
        val publishedAt: LocalDateTime?,
        val url: String
    ) {
        fun toPost(thumbnail: String, publishedAt: LocalDateTime): TechBlogPost = TechBlogPost(
            key = key,
            title = title,
            description = description,
            tags = tags,
            thumbnail = thumbnail,
            publishedAt = publishedAt,
            url = url
        )
    }

    private data class Detail(
        val thumbnail: String,
        val publishedAt: LocalDateTime?
    )

    companion object {
        private const val BLOG_KEY = "ab180"
        private const val RSS_URL =
            "https://raw.githubusercontent.com/ab180/engineering-blog-rss-scheduler/main/rss.xml"
        private const val STORY_PATH_MARKER = "/stories/"
        private const val TIMEOUT_MS = 10_000
    }
}
