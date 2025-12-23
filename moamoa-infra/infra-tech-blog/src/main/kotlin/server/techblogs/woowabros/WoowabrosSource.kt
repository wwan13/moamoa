package server.techblogs.woowabros

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import server.utill.jsoup.fetchWithPaging
import server.utill.webclient.PagingFinishedException
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class WoowabrosSource : TechBlogSource {

    private val baseUrl = "https://techblog.woowahan.com"
    private val timeoutMs = 10_000

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        val listFlow: Flow<TechBlogPost> = fetchWithPaging(size, ::buildListUrl) { doc ->
            val items = doc.select("div.post-item")
            if (items.isEmpty()) throw PagingFinishedException()

            items.mapNotNull { item ->
                val linkEl = item.selectFirst("a[href]:has(h2.post-title)") ?: return@mapNotNull null

                val url = linkEl.absUrl("href")
                if (url.isBlank()) return@mapNotNull null

                val title = linkEl.selectFirst("h2.post-title")?.text()?.trim() ?: return@mapNotNull null

                val description = linkEl.selectFirst("p.post-excerpt")?.text()?.trim().orEmpty()

                val publishedAt = item.selectFirst("time.post-author-date")
                    ?.text()

                    ?.let { parseDateTime(it) }
                    ?: LocalDateTime.MIN

                TechBlogPost(
                    key = extractKey(url),
                    title = title,
                    description = description,
                    categories = emptyList(),
                    thumbnail = "",
                    publishedAt = publishedAt,
                    url = url
                )
            }
        }

        return listFlow.flatMapMerge(concurrency = 10) { base ->
            flow {
                val categories = runCatching {
                    fetchCategories(base.url)
                }.getOrDefault(emptyList())
                emit(base.copy(categories = categories))
            }
        }
    }

    private fun buildListUrl(page: Int): String =
        if (page == 1) baseUrl else "$baseUrl/?paged=$page"

    private suspend fun fetchCategories(postUrl: String): List<String> = withContext(Dispatchers.IO) {
        val doc = get(postUrl)
        doc.select("span.cats a.cat-tag")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun get(url: String): Document =
        Jsoup.connect(url)
            .timeout(timeoutMs)
            .userAgent("Mozilla/5.0 (compatible; HanipBot/1.0)")
            .referrer(baseUrl)
            .followRedirects(true)
            .get()

    private fun extractKey(url: String): String =
        URI(url).path.trimEnd('/').substringAfterLast('/')

    private fun parseDateTime(raw: String): LocalDateTime {
        val text = raw.trim()
        for (formatter in dateFormatters) {
            runCatching { return LocalDate.parse(text, formatter).atStartOfDay() }
        }
        throw IllegalArgumentException("Unsupported date format: $raw")
    }

    private val dateFormatters = listOf(
        DateTimeFormatter.ofPattern("yyyy. MM. dd."),
        DateTimeFormatter.ofPattern("MMM.dd.yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMM. dd. yyyy", Locale.ENGLISH),
    )
}