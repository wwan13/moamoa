package server.techblogs.wanted

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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Component
internal class WantedSource : TechBlogSource {

    private val baseUrl = "https://social.wanted.co.kr/community/team/171"
    private val timeoutMs = 10_000

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        val seenKeys = HashSet<String>(2048)
        var pageNo = 0

        val listFlow: Flow<PostLink> = fetchWithPaging(
            targetCount = size,
            buildUrl = ::buildListUrl,
            timeoutMs = timeoutMs
        ) { doc ->
            pageNo++

            val links = doc.select("a[href^=/community/article/]")
            if (links.isEmpty()) throw PagingFinishedException()

            val items = links.map { link ->
                val url = link.absUrl("href").trim()
                if (url.isBlank()) {
                    throw IllegalStateException("blogKey=$BLOG_KEY url=<unknown> field=url")
                }
                val key = extractKey(url)
                if (key.isBlank()) {
                    throw IllegalStateException("blogKey=$BLOG_KEY url=$url field=key")
                }
                PostLink(key = key, url = url)
            }

            val unique = items.filter { seenKeys.add(it.key) }
            if (unique.isEmpty()) throw PagingFinishedException()

            if (pageNo > 1) throw PagingFinishedException()

            unique
        }

        return listFlow.flatMapMerge(concurrency = 10) { link ->
            flow {
                emit(fetchDetail(link))
            }
        }
    }

    private fun buildListUrl(page: Int): String = baseUrl

    private fun fetchDetail(link: PostLink): TechBlogPost {
        val doc = jsoup(link.url, timeoutMs)

        val title = findTitle(doc)
            ?: throw IllegalStateException("blogKey=$BLOG_KEY url=${link.url} field=title")

        val description = findDescription(doc).orEmpty()

        val thumbnail = findThumbnail(doc)
            ?: throw IllegalStateException("blogKey=$BLOG_KEY url=${link.url} field=thumbnail")

        val publishedAt = findPublishedAt(doc) ?: LocalDateTime.MIN

        return TechBlogPost(
            key = link.key,
            title = title,
            description = description,
            tags = emptyList(),
            thumbnail = thumbnail,
            publishedAt = publishedAt,
            url = link.url
        )
    }

    private fun findTitle(doc: Document): String? {
        return doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim()?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("meta[name=twitter:title]")?.attr("content")?.trim()?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("h1")?.text()?.trim()?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("h2")?.text()?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun findDescription(doc: Document): String? {
        return doc.selectFirst("meta[property=og:description]")?.attr("content")?.trim()?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("meta[name=description]")?.attr("content")?.trim()?.takeIf { it.isNotBlank() }
            ?: doc.select("article p, section p, p")
                .mapNotNull { it.text().trim().takeIf { text -> text.isNotBlank() } }
                .firstOrNull()
    }

    private fun findThumbnail(doc: Document): String? {
        return doc.selectFirst("meta[property=og:image]")?.attr("content")?.trim()?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("meta[property=og:image:secure_url]")?.attr("content")?.trim()?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("meta[name=twitter:image]")?.attr("content")?.trim()?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("article img[src]")?.absUrl("src")?.trim()?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("img[src]")?.absUrl("src")?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun findPublishedAt(doc: Document): LocalDateTime? {
        val meta = doc.selectFirst("meta[property=article:published_time], meta[name=article:published_time]")
            ?.attr("content")
            ?.trim()

        parsePublishedAt(meta)?.let { return it }

        val datetime = doc.selectFirst("time[datetime]")?.attr("datetime")?.trim()
        parsePublishedAt(datetime)?.let { return it }

        val timeText = doc.selectFirst("time")?.text()?.trim()
        parsePublishedAt(timeText)?.let { return it }

        val textDate = dateRegex.find(doc.text())?.value
        return parsePublishedAt(textDate)
    }

    private fun parsePublishedAt(raw: String?): LocalDateTime? {
        val text = raw?.trim().orEmpty()
        if (text.isBlank()) return null

        runCatching { return OffsetDateTime.parse(text).toLocalDateTime() }
        runCatching { return LocalDateTime.parse(text) }
        runCatching { return LocalDate.parse(text).atStartOfDay() }

        DATE_FORMATTERS.forEach { formatter ->
            runCatching { return LocalDate.parse(text, formatter).atStartOfDay() }
        }

        return null
    }

    private fun extractKey(url: String): String =
        URI(url).path.trimEnd('/').substringAfterLast('/').trim()

    private data class PostLink(
        val key: String,
        val url: String
    )

    companion object {
        private const val BLOG_KEY = "wanted"
        private val DATE_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy. M. d")
        )
        private val dateRegex = Regex("""\d{4}\.\d{2}\.\d{2}""")
    }
}
