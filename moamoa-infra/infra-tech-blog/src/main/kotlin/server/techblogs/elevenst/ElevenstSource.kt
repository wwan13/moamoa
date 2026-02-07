package server.techblogs.elevenst

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import server.utill.PagingFinishedException
import server.utill.fetchWithPaging
import server.utill.jsoup
import server.utill.normalizeTagTitle
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Component
internal class ElevenstSource : TechBlogSource {

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        val seenKeys = HashSet<String>(2048)

        val listFlow: Flow<BasicPost> = fetchWithPaging(
            targetCount = size,
            buildUrl = ::buildListUrl,
            timeoutMs = TIMEOUT_MS
        ) { doc ->
            val items = doc.select("ul#post-list > li.post-item")
            if (items.isEmpty()) throw PagingFinishedException()

            val parsed = items.mapNotNull { item ->
                val linkEl = item.selectFirst("a[href]:has(h3.post-title)") ?: item.selectFirst("a[href]")
                val url = linkEl?.absUrl("href")?.trim().orEmpty()
                if (url.isBlank()) return@mapNotNull null

                val title = item.selectFirst("h3.post-title")?.text()?.trim().orEmpty()
                if (title.isBlank()) return@mapNotNull null

                val description = item.selectFirst("p.post-excerpt")?.text()?.trim().orEmpty()

                val tags = item.select("p.post-tags a.tag, p.post-tags a")
                    .mapNotNull { it.text().normalizeTagTitle().takeIf(String::isNotBlank) }
                    .distinct()

                BasicPost(
                    key = extractKey(url),
                    title = title,
                    description = description,
                    tags = tags,
                    url = url
                )
            }

            val unique = parsed.filter { seenKeys.add(it.key) }
            if (unique.isEmpty()) throw PagingFinishedException()
            unique
        }

        return listFlow.flatMapMerge(concurrency = 10) { base ->
            flow {
                val publishedAt = runCatching {
                    fetchPublishedAtFromDetail(base.url)
                }.getOrDefault(LocalDateTime.MIN)

                emit(
                    TechBlogPost(
                        key = base.key,
                        title = base.title,
                        description = base.description,
                        tags = base.tags,
                        thumbnail = DEFAULT_THUMBNAIL,
                        publishedAt = publishedAt,
                        url = base.url
                    )
                )
            }
        }
    }

    private fun buildListUrl(page: Int): String =
        if (page == 1) BASE_URL else "$BASE_URL/page/$page/"

    private fun extractKey(url: String): String =
        URI(url).path.trimEnd('/')

    private suspend fun fetchPublishedAtFromDetail(url: String): LocalDateTime = withContext(Dispatchers.IO) {
        val doc = runCatching { jsoup(url, TIMEOUT_MS) }.getOrNull() ?: return@withContext LocalDateTime.MIN

        val metaDate = doc.selectFirst("meta[property=article:published_time]")
            ?.attr("content")
            ?.trim()
        if (!metaDate.isNullOrBlank()) return@withContext parsePublishedAt(metaDate)

        val postDate = doc.selectFirst("#post-date, .post-date")
            ?.text()
            ?.trim()
        if (!postDate.isNullOrBlank()) return@withContext parsePublishedAt(postDate)

        val timeAttr = doc.selectFirst("time[datetime]")
            ?.attr("datetime")
            ?.trim()
        if (!timeAttr.isNullOrBlank()) return@withContext parsePublishedAt(timeAttr)

        return@withContext LocalDateTime.MIN
    }

    private fun parsePublishedAt(raw: String): LocalDateTime {
        val text = raw.trim()
        if (text.isBlank()) return LocalDateTime.MIN

        runCatching { return OffsetDateTime.parse(text).toLocalDateTime() }
        runCatching { return LocalDate.parse(text, DateTimeFormatter.ISO_DATE).atStartOfDay() }
        return LocalDateTime.MIN
    }

    private companion object {
        private const val BASE_URL = "https://11st-tech.github.io"
        private const val TIMEOUT_MS = 10_000
        private const val DEFAULT_THUMBNAIL = "https://i.imgur.com/CZ9EMKu.png"
    }

    private data class BasicPost(
        val key: String,
        val title: String,
        val description: String,
        val tags: List<String>,
        val url: String
    )
}
