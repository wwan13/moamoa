package server.techblogs.samsung

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
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
internal class SamsungSource : TechBlogSource {

    private val baseUrl = "https://techblog.samsung.com"
    private val timeoutMs = 10_000

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        val listFlow: Flow<TechBlogPost> = fetchWithPaging(size, ::buildListUrl, timeoutMs = timeoutMs) { doc ->
            val items = doc.select("ul.blog-list > li")
            if (items.isEmpty()) throw PagingFinishedException()

            items.mapNotNull { item ->
                val linkEl = item.selectFirst("a[href^=/blog/article/]") ?: return@mapNotNull null

                val url = linkEl.absUrl("href")
                if (url.isBlank()) return@mapNotNull null

                val key = extractKey(url)
                if (key.isBlank()) return@mapNotNull null

                val title = item.selectFirst("h3")
                    ?.text()
                    ?.trim()
                    ?: return@mapNotNull null

                val publishedAt = item.selectFirst("span.date")
                    ?.text()
                    ?.trim()
                    ?.let(::parseDateTime)
                    ?: LocalDateTime.MIN

                val thumbnail = item.selectFirst("img")
                    ?.absUrl("src")
                    ?.takeIf { it.isNotBlank() }
                    ?: DEFAULT_THUMBNAIL

                TechBlogPost(
                    key = key,
                    title = title,
                    description = "",
                    tags = emptyList(),
                    thumbnail = thumbnail,
                    publishedAt = publishedAt,
                    url = url
                )
            }
        }

        return listFlow.flatMapMerge(concurrency = 10) { base ->
            flow {
                val detail = runCatching { fetchDetail(base.url) }
                    .getOrNull()
                    ?: DetailInfo(emptyList(), "")

                emit(
                    base.copy(
                        tags = detail.tags,
                        description = detail.description.ifBlank { base.description }
                    )
                )
            }
        }
    }

    private fun buildListUrl(page: Int): String = "$baseUrl/?page=$page&"

    private suspend fun fetchDetail(postUrl: String): DetailInfo = withContext(Dispatchers.IO) {
        val doc = jsoup(postUrl, timeoutMs = timeoutMs)

        val description = doc.selectFirst("article.txt-group p strong")
            ?.text()
            ?.trim()
            .orEmpty()

        val tags = doc.select("p.tag-list a[href*='tagName=']")
            .mapNotNull { it.text().trim().removePrefix("#").normalizeTagTitle() }
            .filter { it.isNotBlank() }
            .distinct()

        DetailInfo(tags = tags, description = description)
    }

    private fun extractKey(url: String): String =
        runCatching { URI(url).path.trimEnd('/').substringAfterLast('/') }.getOrDefault("")

    private fun parseDateTime(raw: String): LocalDateTime =
        LocalDate.parse(raw.trim(), DATE_FORMATTER).atStartOfDay()

    private data class DetailInfo(
        val tags: List<String>,
        val description: String
    )

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
        private const val DEFAULT_THUMBNAIL = "https://i.imgur.com/Qm5FpnX.png"
    }
}
