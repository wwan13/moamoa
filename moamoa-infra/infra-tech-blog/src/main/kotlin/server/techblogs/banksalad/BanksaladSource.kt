package server.techblogs.banksalad

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Element
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
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
internal class BanksaladSource : TechBlogSource {

    private val blogKey = "banksalad"
    private val baseUrl = "https://blog.banksalad.com"
    private val timeoutMs = 10_000

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        val seenKeys = HashSet<String>(2048)

        val listFlow: Flow<TechBlogPost> = fetchWithPaging(size, ::buildListUrl, timeoutMs = timeoutMs) { doc ->
            val cards = doc.select(".post_card")
            if (cards.isEmpty()) throw PagingFinishedException()

            val parsed = cards.map { card ->
                val titleEl = card.selectFirst(".post_title a[href^=/tech/]")
                    ?: throw IllegalStateException("blogKey=$blogKey url=<unknown> field=url")
                val href = titleEl.attr("href").trim()
                if (href.isBlank()) {
                    throw IllegalStateException("blogKey=$blogKey url=<unknown> field=url")
                }
                val url = toAbsoluteUrl(href)
                if (url.isBlank()) {
                    throw IllegalStateException("blogKey=$blogKey url=<unknown> field=url")
                }

                val key = extractKey(url)
                if (key.isBlank()) {
                    throw IllegalStateException("blogKey=$blogKey url=$url field=key")
                }

                val title = titleEl.text().trim()
                if (title.isBlank()) {
                    throw IllegalStateException("blogKey=$blogKey url=$url field=title")
                }

                val description = card.selectFirst(".excerpt")
                    ?.text()
                    ?.trim()
                    .orEmpty()

                val tags = card.select(".post_tags a[href^=/tags/]")
                    .mapNotNull { it.text().trim().removePrefix("#").normalizeTagTitle().ifBlank { null } }
                    .filter { it.isNotBlank() }
                    .distinct()

                val thumbnail = extractThumbnail(card)
                if (thumbnail.isBlank()) {
                    throw IllegalStateException("blogKey=$blogKey url=$url field=thumbnail")
                }

                TechBlogPost(
                    key = key,
                    title = title,
                    description = description,
                    tags = tags,
                    thumbnail = thumbnail,
                    publishedAt = LocalDateTime.MIN,
                    url = url
                )
            }

            val unique = parsed.filter { seenKeys.add(it.key) }
            if (unique.isEmpty()) throw PagingFinishedException()

            unique
        }

        return listFlow.flatMapMerge(concurrency = 10) { base ->
            flow {
                val detail = runCatching { fetchDetail(base.url) }
                    .getOrDefault(DetailInfo(LocalDateTime.MIN, ""))

                val publishedAt = if (detail.publishedAt == LocalDateTime.MIN) base.publishedAt else detail.publishedAt
                val resolvedThumbnail = if (base.thumbnail.isBlank()) detail.thumbnail else base.thumbnail
                if (resolvedThumbnail.isBlank()) {
                    throw IllegalStateException("blogKey=$blogKey url=${base.url} field=thumbnail")
                }

                emit(base.copy(publishedAt = publishedAt, thumbnail = resolvedThumbnail))
            }
        }
    }

    private fun buildListUrl(page: Int): String = "$baseUrl/tech/page/$page/"

    private suspend fun fetchDetail(postUrl: String): DetailInfo = withContext(Dispatchers.IO) {
        val doc = jsoup(postUrl, timeoutMs = timeoutMs)

        val publishedAt = doc.selectFirst(".post_details > span")
            ?.text()
            ?.trim()
            ?.let(::parsePublishedAt)
            ?: LocalDateTime.MIN

        val thumbnail = doc.selectFirst("meta[property=og:image]")
            ?.attr("content")
            ?.trim()
            .orEmpty()

        DetailInfo(publishedAt = publishedAt, thumbnail = thumbnail)
    }

    private fun extractThumbnail(card: Element): String {
        val img = card.selectFirst(".post_preview img[data-main-image]")
            ?: card.selectFirst(".post_preview img[alt]")
        val raw = img?.attr("data-src")?.trim().takeIf { it?.isNotBlank() ?: false }
            ?: img?.attr("src")?.trim().takeIf { it?.isNotBlank() ?: false }
            ?: ""
        return toAbsoluteUrl(raw)
    }

    private fun extractKey(url: String): String =
        runCatching { URI(url).path.trimEnd('/').substringAfterLast('/') }.getOrDefault("")

    private fun parsePublishedAt(raw: String): LocalDateTime =
        LocalDate.parse(raw.trim(), DETAIL_DATE_FORMATTER).atStartOfDay()

    private fun toAbsoluteUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""
        return when {
            trimmed.startsWith("http") -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("/") -> baseUrl + trimmed
            else -> "$baseUrl/$trimmed"
        }
    }

    private data class DetailInfo(
        val publishedAt: LocalDateTime,
        val thumbnail: String
    )

    companion object {
        private val DETAIL_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH)
    }
}
