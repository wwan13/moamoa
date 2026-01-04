package server.techblogs.kakaopay

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
import java.lang.IllegalStateException
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class KakaoPaySource : TechBlogSource {

    private val baseUrl = "https://tech.kakaopay.com"
    private val timeoutMs = 10_000

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        val listFlow = fetchWithPaging(size, ::buildListUrl, timeoutMs = timeoutMs) { doc ->
            val items = doc.select("li:has(a[href^=/post/])")
            if (items.isEmpty()) throw PagingFinishedException()

            items.mapNotNull { item ->
                val linkEl = item.selectFirst("a[href^=/post/]") ?: return@mapNotNull null
                val url = linkEl.absUrl("href")
                if (url.isBlank()) return@mapNotNull null

                val title = item.selectFirst("strong")
                    ?.text()
                    ?.trim()
                    ?: return@mapNotNull null

                val description = item.selectFirst("p")
                    ?.text()
                    ?.trim()
                    .orEmpty()

                val publishedAt = item.selectFirst("time")
                    ?.text()
                    ?.trim()
                    ?.let(::parseDateTime)
                    ?: LocalDateTime.MIN

                val thumbnail = item.selectFirst("img[alt]")
                    ?.absUrl("src")
                    ?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("no thumbnail")

                TechBlogPost(
                    key = extractKey(url),
                    title = title,
                    description = description,
                    categories = emptyList(),
                    thumbnail = thumbnail,
                    publishedAt = publishedAt,
                    url = url
                )
            }
        }

        return listFlow.flatMapMerge(concurrency = 10) { base ->
            flow {
                val categories = runCatching { fetchCategories(base.url) }
                    .getOrDefault(emptyList())
                emit(base.copy(categories = categories))
            }
        }
    }

    private fun buildListUrl(page: Int): String =
        if (page == 1) baseUrl else "$baseUrl/page/$page/"

    private suspend fun fetchCategories(postUrl: String): List<String> = withContext(Dispatchers.IO) {
        val doc = jsoup(postUrl, timeoutMs = timeoutMs)
        doc.select("a[href^=/tag/]")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun extractKey(url: String): String =
        URI(url).path.trimEnd('/').substringAfterLast('/')

    private fun parseDateTime(raw: String): LocalDateTime =
        LocalDate.parse(raw.trim(), DATE_FORMATTER).atStartOfDay()

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy. M. d")
    }
}