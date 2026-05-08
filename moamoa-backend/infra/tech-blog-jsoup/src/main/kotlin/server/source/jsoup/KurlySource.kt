package server.source.jsoup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
internal class KurlySource : TechBlogSource {

    private val baseUrl = "https://helloworld.kurly.com"
    private val timeoutMs = 10_000
    private val legacyDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    private val koreanDatePattern = Regex("""(\d{4})년\s*(\d{1,2})월\s*(\d{1,2})일""")
    private val seoulZone = ZoneId.of("Asia/Seoul")

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        var pageNo = 0
        var firstPageSignature: String? = null

        return fetchWithPaging(
            targetCount = size,
            buildUrl = ::buildListUrl,
            timeoutMs = timeoutMs
        ) { doc ->
            pageNo++

            val posts = parsePosts(doc)
            if (posts.isEmpty()) throw PagingFinishedException()

            val signature = posts.map { it.url.trim() }
                .filter { it.isNotBlank() }
                .joinToString("|")

            if (signature.isBlank()) throw PagingFinishedException()
            if (pageNo == 1) firstPageSignature = signature
            if (pageNo > 1 && firstPageSignature == signature) throw PagingFinishedException()

            posts
        }
    }

    internal fun parsePosts(doc: Document): List<TechBlogPost> {
        val currentCards = doc.select("article a[href]")
            .filter { it.normalizedBlogUrl().isNotBlank() }
        val legacyCards = doc.select("ul.post-list li.post-card")

        return if (currentCards.isNotEmpty()) {
            currentCards.mapNotNull(::parseCurrentCard)
        } else {
            legacyCards.mapNotNull(::parseLegacyCard)
        }
    }

    private fun parseCurrentCard(card: Element): TechBlogPost? {
        val url = card.normalizedBlogUrl().ifBlank { return null }
        val title = card.selectFirst("h2")?.normalizedText() ?: return null
        val description = card.selectFirst("p")?.normalizedText().orEmpty()
        val category = card.selectFirst("span")?.normalizedText()
        val time = card.selectFirst("time")
        val publishedAt = parsePublishedAt(
            raw = time?.text()?.trim().orEmpty(),
            datetime = time?.attr("datetime")?.trim().orEmpty()
        ) ?: return null

        return TechBlogPost(
            key = extractKey(url),
            title = title,
            description = description,
            tags = listOfNotNull(category),
            thumbnail = card.selectFirst("img[src]")?.absUrl("src").orEmpty(),
            publishedAt = publishedAt,
            url = url
        )
    }

    private fun parseLegacyCard(card: Element): TechBlogPost? {
        val link = card.selectFirst("a.post-link[href]") ?: return null
        val url = link.absUrl("href").trim().ifBlank { return null }
        val title = card.selectFirst("h3.post-title")?.normalizedText() ?: return null
        val description = card.selectFirst("p.title-summary")?.normalizedText().orEmpty()
        val publishedAtRaw = card.selectFirst("span.post-date")?.text()?.trim()?.trimEnd('.') ?: return null
        val publishedAt = parsePublishedAt(raw = publishedAtRaw, datetime = "") ?: return null

        return TechBlogPost(
            key = extractKey(url),
            title = title,
            description = description,
            tags = emptyList(),
            thumbnail = "",
            publishedAt = publishedAt,
            url = url
        )
    }

    private fun buildListUrl(page: Int): String =
        if (page == 1) "$baseUrl/" else "$baseUrl/page/$page/"

    private fun extractKey(url: String): String =
        URI(url).path.trimEnd('/').substringAfterLast('/')

    private fun parsePublishedAt(raw: String, datetime: String): LocalDateTime? {
        koreanDatePattern.find(raw)?.let { match ->
            val (year, month, day) = match.destructured
            return LocalDate.of(year.toInt(), month.toInt(), day.toInt()).atStartOfDay()
        }

        runCatching {
            LocalDate.parse(raw.trimEnd('.'), legacyDateFormatter).atStartOfDay()
        }.getOrNull()?.let {
            return it
        }

        return runCatching {
            OffsetDateTime.parse(datetime)
                .atZoneSameInstant(seoulZone)
                .toLocalDate()
                .atStartOfDay()
        }.getOrNull()
    }

    private fun Element.normalizedText(): String? =
        text().trim().takeIf { it.isNotBlank() }

    private fun Element.normalizedBlogUrl(): String =
        absUrl("href").trim().takeIf { url ->
            runCatching { URI(url).path.startsWith("/blog/") }.getOrDefault(false)
        }.orEmpty()
}
