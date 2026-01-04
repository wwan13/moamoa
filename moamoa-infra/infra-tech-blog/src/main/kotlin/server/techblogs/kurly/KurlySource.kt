package server.techblogs.kurly

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import server.utill.PagingFinishedException
import server.utill.fetchWithPaging
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class KurlySource : TechBlogSource {

    private val baseUrl = "https://helloworld.kurly.com"
    private val timeoutMs = 10_000
    private val fixedThumbnail = "https://i.imgur.com/qEmALiB.png"

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

            val cards = doc.select("ul.post-list li.post-card")
            if (cards.isEmpty()) throw PagingFinishedException()

            // ✅ 현재 페이지 시그니처(상대/절대 href 정규화)
            val signature = cards.mapNotNull {
                it.selectFirst("a.post-link[href]")?.absUrl("href")?.trim()
            }
                .filter { it.isNotBlank() }
                .joinToString("|")

            if (signature.isBlank()) throw PagingFinishedException()
            if (pageNo == 1) firstPageSignature = signature
            if (pageNo > 1 && firstPageSignature == signature) throw PagingFinishedException()

            cards.mapNotNull { card ->
                val link = card.selectFirst("a.post-link[href]") ?: return@mapNotNull null
                val url = link.absUrl("href").trim().ifBlank { return@mapNotNull null }

                val title = card.selectFirst("h3.post-title")
                    ?.text()
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null

                val description = card.selectFirst("p.title-summary")
                    ?.text()
                    ?.trim()
                    .orEmpty()

                val publishedAtRaw = card.selectFirst("span.post-date")
                    ?.text()
                    ?.trim()
                    ?.trimEnd('.')
                    ?: return@mapNotNull null

                val publishedAt = parsePublishedAt(publishedAtRaw)

                TechBlogPost(
                    key = extractKey(url),
                    title = title,
                    description = description,
                    categories = emptyList(),
                    thumbnail = fixedThumbnail,
                    publishedAt = publishedAt,
                    url = url
                )
            }
        }
    }

    // Kurly는 /blog/... 로 계속 이어짐(페이지네이션이 있으면 여기만 바꾸면 됨)
    private fun buildListUrl(page: Int): String =
        if (page == 1) "$baseUrl/" else "$baseUrl/page/$page/"

    private fun extractKey(url: String): String =
        URI(url).path.trimEnd('/').substringAfterLast('/')

    private fun parsePublishedAt(raw: String): LocalDateTime {
        // 예: "2025.12.24" (마지막 점은 위에서 제거)
        val date = LocalDate.parse(raw, DateTimeFormatter.ofPattern("yyyy.MM.dd"))
        return date.atStartOfDay()
    }
}