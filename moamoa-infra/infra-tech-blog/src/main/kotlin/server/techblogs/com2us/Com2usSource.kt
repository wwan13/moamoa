package server.techblogs.com2us

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import server.utill.PagingFinishedException
import server.utill.fetchWithPaging
import server.utill.jsoup
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
internal class Com2usSource : TechBlogSource {

    private val baseUrl = "https://on.com2us.com/tag/%EA%B8%B0%EC%88%A0%EB%B8%94%EB%A1%9C%EA%B7%B8/"
    private val timeoutMs = 10_000

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        val listFlow: Flow<BasicPost> = fetchWithPaging(
            targetCount = size,
            buildUrl = ::buildListUrl,
            timeoutMs = timeoutMs
        ) { doc ->
            val items = extractPosts(doc)
            if (items.isEmpty()) throw PagingFinishedException()
            items
        }

        return listFlow.flatMapMerge(concurrency = 10) { base ->
            flow {
                val publishedAt = fetchPublishedAt(base.url) ?: LocalDateTime.MIN
                emit(
                    TechBlogPost(
                        key = extractKey(base.url),
                        title = base.title,
                        description = base.description,
                        tags = base.tags,
                        thumbnail = base.thumbnail,
                        publishedAt = publishedAt,
                        url = base.url
                    )
                )
            }
        }
    }

    private fun buildListUrl(page: Int): String =
        if (page == 1) baseUrl else "${baseUrl}page/$page/"

    private fun extractPosts(doc: Document): List<BasicPost> {
        return doc.select("section.archive-list a.loop-grid.loop[href]")
            .mapNotNull { link ->
                val url = link.absUrl("href").trim()
                if (url.isBlank()) return@mapNotNull null

                val title = link.selectFirst("div.title h4")
                    ?.text()
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: link.selectFirst("h4")
                        ?.text()
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }

                if (title.isNullOrBlank()) return@mapNotNull null

                val description = link.selectFirst("div.content p")
                    ?.text()
                    ?.trim()
                    .orEmpty()

                val thumbnail = link.selectFirst("div.image img[src]")
                    ?.absUrl("src")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: link.selectFirst("div.image img[src]")
                        ?.attr("src")
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                    ?: DEFAULT_THUMBNAIL

                val tags = link.select("div.tags li span")
                    .mapNotNull { it.text().trim().takeIf { tag -> tag.isNotBlank() } }
                    .distinct()

                BasicPost(
                    url = url,
                    title = title,
                    description = description,
                    tags = tags,
                    thumbnail = thumbnail
                )
            }
    }

    private fun fetchPublishedAt(url: String): LocalDateTime? {
        val doc = jsoup(url, timeoutMs)

        val meta = doc.selectFirst("meta[property=article:published_time], meta[name=article:published_time]")
            ?.attr("content")
            ?.trim()

        parsePublishedAt(meta)?.let { return it }

        val datetime = doc.selectFirst("time[datetime]")?.attr("datetime")?.trim()
        parsePublishedAt(datetime)?.let { return it }

        return null
    }

    private fun parsePublishedAt(raw: String?): LocalDateTime? {
        val text = raw?.trim().orEmpty()
        if (text.isBlank()) return null

        runCatching { return OffsetDateTime.parse(text).toLocalDateTime() }
        runCatching { return Instant.parse(text).atZone(ZoneOffset.UTC).toLocalDateTime() }
        runCatching { return LocalDateTime.parse(text) }
        runCatching { return LocalDate.parse(text).atStartOfDay() }

        return null
    }

    private fun extractKey(url: String): String =
        URI(url).path.trimEnd('/').substringAfterLast('/').trim()

    private data class BasicPost(
        val url: String,
        val title: String,
        val description: String,
        val tags: List<String>,
        val thumbnail: String
    )

    companion object {
        private const val DEFAULT_THUMBNAIL = "https://i.imgur.com/xyE7Tom.png"
    }
}
