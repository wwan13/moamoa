package server.techblogs.saramin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
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
import java.time.format.DateTimeFormatter

@Component
internal class SaraminSource : TechBlogSource {

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        val seenUrls = mutableSetOf<String>()
        var lastSignature: String? = null

        val listFlow: Flow<String> = fetchWithPaging(
            targetCount = size,
            buildUrl = ::buildListUrl,
            timeoutMs = TIMEOUT_MS
        ) { doc ->
            val postUrls = extractPostUrls(doc)
            if (postUrls.isEmpty()) throw PagingFinishedException()

            val signature = postUrls.joinToString("|")
            if (signature.isBlank()) throw PagingFinishedException()
            if (lastSignature != null && lastSignature == signature) throw PagingFinishedException()
            lastSignature = signature

            val newUrls = postUrls.filter { seenUrls.add(it) }
            if (newUrls.isEmpty()) throw PagingFinishedException()
            newUrls
        }

        return listFlow.flatMapMerge(concurrency = 10) { url ->
            flow { emit(fetchPost(url)) }
        }
    }

    private fun buildListUrl(page: Int): String =
        if (page == 1) "$BASE_URL/" else "$BASE_URL/page$page/"

    private fun extractPostUrls(doc: Document): List<String> {
        return doc.select("a[href]")
            .mapNotNull { it.absUrl("href").trim() }
            .filter { it.isNotBlank() }
            .filter { POST_URL_REGEX.matches(it) }
            .distinct()
    }

    private suspend fun fetchPost(url: String): TechBlogPost = withContext(Dispatchers.IO) {
        val normalizedUrl = url.trim()
        if (normalizedUrl.isBlank()) {
            throw IllegalStateException("blogKey=$BLOG_KEY url=unknown field=url")
        }

        val doc = jsoup(normalizedUrl, TIMEOUT_MS)

        val title = extractTitle(doc)
            ?: throw IllegalStateException("blogKey=$BLOG_KEY url=$normalizedUrl field=title")

        val description = extractDescription(doc)
        val thumbnail = extractThumbnail(doc)

        val publishedAt = extractPublishedAt(doc, normalizedUrl)
        val key = extractKey(normalizedUrl)

        TechBlogPost(
            key = key,
            title = title,
            description = description,
            tags = emptyList(),
            thumbnail = thumbnail,
            publishedAt = publishedAt,
            url = normalizedUrl
        )
    }

    private fun extractTitle(doc: Document): String? {
        return doc.selectFirst("meta[property=og:title]")
            ?.attr("content")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("meta[name=twitter:title]")
                ?.attr("content")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("h1")
                ?.text()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
    }

    private fun extractDescription(doc: Document): String {
        return doc.selectFirst("meta[property=og:description]")
            ?.attr("content")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("meta[name=description]")
                ?.attr("content")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            ?: doc.select("article p, main p, section p, p")
                .firstOrNull { it.text().trim().isNotBlank() }
                ?.text()
                ?.trim()
                .orEmpty()
    }

    private fun extractThumbnail(doc: Document): String {
        val metaImage = doc.selectFirst("meta[property=og:image]")
            ?.attr("content")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("meta[name=twitter:image]")
                ?.attr("content")
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        if (!metaImage.isNullOrBlank()) return resolveUrl(metaImage)

        val image = doc.selectFirst("article img[src], main img[src], section img[src]")
            ?.absUrl("src")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("article img[src], main img[src], section img[src]")
                ?.attr("src")
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        return image?.let { resolveUrl(it) } ?: "https://i.imgur.com/CvjQExf.png"
    }

    private fun extractPublishedAt(doc: Document, url: String): LocalDateTime {
        val meta = doc.selectFirst("meta[property=article:published_time]")
            ?.attr("content")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("time[datetime]")
                ?.attr("datetime")
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        val fromMeta = meta?.let { parseDateTime(it) }
        return fromMeta ?: parseDateFromUrl(url) ?: LocalDateTime.MIN
    }

    private fun parseDateTime(raw: String): LocalDateTime? {
        val text = raw.trim()
        if (text.isBlank()) return null

        runCatching { return Instant.parse(text).atZone(ZoneOffset.UTC).toLocalDateTime() }
        runCatching { return OffsetDateTime.parse(text).toLocalDateTime() }
        runCatching { return LocalDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME) }
        runCatching { return LocalDate.parse(text, DateTimeFormatter.ISO_DATE).atStartOfDay() }
        return null
    }

    private fun parseDateFromUrl(url: String): LocalDateTime? {
        val matched = URL_DATE_REGEX.find(url)
            ?: URL_DATE_SLASH_REGEX.find(url)
            ?: return null

        val (year, month, day) = matched.destructured
        return runCatching {
            LocalDate.of(year.toInt(), month.toInt(), day.toInt()).atStartOfDay()
        }.getOrNull()
    }

    private fun extractKey(url: String): String {
        val key = URI(url).path.trimEnd('/').substringAfterLast('/').trim()
        if (key.isBlank()) throw IllegalStateException("blogKey=$BLOG_KEY url=$url field=key")
        return key
    }

    private fun resolveUrl(candidate: String): String {
        val trimmed = candidate.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        return "$BASE_URL/${trimmed.trimStart('/')}"
    }

    companion object {
        private const val BLOG_KEY = "saramin"
        private const val BASE_URL = "https://saramin.github.io"
        private const val TIMEOUT_MS = 10_000

        private val POST_URL_REGEX = Regex("""https://saramin\.github\.io/\d{4}-\d{2}-\d{2}-[\w\-]+/?""")
        private val URL_DATE_REGEX = Regex("""/(\d{4})-(\d{2})-(\d{2})-""")
        private val URL_DATE_SLASH_REGEX = Regex("""/(\d{4})/(\d{2})/(\d{2})/""")
    }
}
