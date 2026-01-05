package server.techblogs.buzzvil

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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
class BuzzvilSource : TechBlogSource {

    private val baseUrl = "https://tech.buzzvil.com"
    private val timeoutMs = 10_000

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {

        var pageNo = 0
        var firstPageSignature: String? = null

        val listFlow: Flow<TechBlogPost> = fetchWithPaging(
            targetCount = size,
            buildUrl = ::buildListUrl,
            timeoutMs = timeoutMs
        ) { doc ->
            pageNo++

            val articles = doc.select("article.post-card")
            if (articles.isEmpty()) throw PagingFinishedException()

            // ✅ 현재 페이지 시그니처(글 URL 목록)
            val signature = articles.mapNotNull {
                it.selectFirst("a.post-title[href]")?.absUrl("href")?.trim()
            }
                .filter { it.isNotBlank() }
                .joinToString("|")

            if (signature.isBlank()) throw PagingFinishedException()

            // ✅ 첫 페이지 시그니처 저장
            if (pageNo == 1) firstPageSignature = signature

            // ✅ 1페이지가 아닌데 1페이지와 동일하면 종료
            if (pageNo > 1 && firstPageSignature == signature) throw PagingFinishedException()

            articles.mapNotNull { article ->
                val titleEl = article.selectFirst("a.post-title[href]") ?: return@mapNotNull null
                val url = titleEl.absUrl("href").trim().ifBlank { return@mapNotNull null }

                val title = titleEl.text().trim().ifBlank { return@mapNotNull null }

                val description = article.selectFirst(".card-body > p")
                    ?.text()
                    ?.trim()
                    .orEmpty()

                val publishedAt = article.select("ul.card-meta span")
                    .map { it.text().trim() }
                    .firstOrNull { it.matches(dateRegex) }
                    ?.let { parsePublishedAt(it) }
                    ?: return@mapNotNull null

                val thumbnail =
                    extractBgImageUrl(article.selectFirst("div.featured-card-image-wrapper")?.attr("style"))
                        ?: extractBgImageUrl(article.selectFirst("a.list-card-image-link")?.attr("style"))
                        ?: (url.trimEnd('/') + "/cover.jpg")

                TechBlogPost(
                    key = extractKey(url),
                    title = title,
                    description = description,
                    tags = emptyList(),
                    thumbnail = thumbnail,
                    publishedAt = publishedAt,
                    url = url
                )
            }
        }

        return listFlow.flatMapMerge(concurrency = 10) { base ->
            flow {
                val categories = runCatching { fetchCategories(base.url) }.getOrDefault(emptyList())
                emit(base.copy(tags = categories))
            }
        }
    }

    private fun buildListUrl(page: Int): String =
        if (page == 1) "$baseUrl/" else "$baseUrl/page/$page/"

    private suspend fun fetchCategories(postUrl: String): List<String> = withContext(Dispatchers.IO) {
        val doc: Document = jsoup(postUrl, timeoutMs)

        // 예시로 준: <ul class="post-meta-tags ..."><a>design</a>...</ul>
        doc.select("ul.post-meta-tags a[href]")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun extractKey(url: String): String =
        URI(url).path.trimEnd('/').substringAfterLast('/')

    private fun parsePublishedAt(raw: String): LocalDateTime {
        val text = raw.trim()
        // 예: "19 Dec, 2025"
        return LocalDate.parse(text, listDateFormatter).atStartOfDay()
    }

    private fun extractBgImageUrl(style: String?): String? {
        if (style.isNullOrBlank()) return null
        // background-image: url("https://.../cover.jpg");
        val m = bgUrlRegex.find(style) ?: return null
        return m.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    private val listDateFormatter = DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH)
    private val bgUrlRegex = Regex("""background-image:\s*url\(["']?([^"')]+)["']?\)""", RegexOption.IGNORE_CASE)
    private val dateRegex = Regex("""\d{2}\s+[A-Za-z]{3},\s+\d{4}""")
}