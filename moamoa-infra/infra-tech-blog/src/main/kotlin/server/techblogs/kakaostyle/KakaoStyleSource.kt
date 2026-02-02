package server.techblogs.kakaostyle

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
import java.util.Locale

@Component
internal class KakaoStyleSource : TechBlogSource {

    private val baseUrl = "https://devblog.kakaostyle.com/ko"
    private val timeoutMs = 10_000

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        var pageNo = 0
        var firstPageSignature: String? = null
        val seenKeys = HashSet<String>(2048)

        return fetchWithPaging(
            targetCount = size,
            buildUrl = ::buildListUrl,
            timeoutMs = timeoutMs
        ) { doc ->
            pageNo++

            val heads = doc.select("div.posts-head")
            if (heads.isEmpty()) throw PagingFinishedException()

            val signature = heads.mapNotNull { head ->
                head.selectFirst("a[href^=/ko/]")?.absUrl("href")?.trim()
            }
                .filter { it.isNotBlank() }
                .joinToString("|")

            if (signature.isBlank()) throw PagingFinishedException()

            if (pageNo == 1) firstPageSignature = signature
            if (pageNo > 1 && firstPageSignature == signature) throw PagingFinishedException()

            val parsed = heads.mapNotNull { head ->
                val parent = head.parent() ?: return@mapNotNull null
                val titleLink = head.selectFirst("a[href^=/ko/]")
                    ?: throw IllegalStateException("blogKey=kakaostyle url=unknown field=url")

                val url = titleLink.absUrl("href").trim()
                if (url.isBlank()) throw IllegalStateException("blogKey=kakaostyle url=unknown field=url")

                val title = titleLink.text().trim()
                    .ifBlank { throw IllegalStateException("blogKey=kakaostyle url=$url field=title") }

                val key = extractKey(url)
                if (key.isBlank()) throw IllegalStateException("blogKey=kakaostyle url=$url field=key")

                val description = parent.selectFirst("div.card-body a.posts-content")
                    ?.text()
                    ?.trim()
                    .orEmpty()

                val publishedAt = head.selectFirst("span.posts-date")
                    ?.text()
                    ?.replace("|", "")
                    ?.trim()
                    ?.let(::parseDate)
                    ?: LocalDateTime.MIN

                TechBlogPost(
                    key = key,
                    title = title,
                    description = description,
                    tags = emptyList(),
                    thumbnail = "https://i.imgur.com/CZ9EMKu.png",
                    publishedAt = publishedAt,
                    url = url
                )
            }

            val unique = parsed.filter { seenKeys.add(it.key) }
            if (unique.isEmpty()) throw PagingFinishedException()

            unique
        }
    }

    private fun buildListUrl(page: Int): String =
        if (page == 1) "$baseUrl/" else "$baseUrl/page/$page/"

    private fun extractKey(url: String): String {
        val path = URI(url).path.trimEnd('/')
        return path.removePrefix("/ko/").trim()
    }

    private fun parseDate(raw: String): LocalDateTime? {
        val value = raw.trim()
        return dateFormatters.firstNotNullOfOrNull { formatter ->
            runCatching { LocalDate.parse(value, formatter).atStartOfDay() }.getOrNull()
        }
    }

    private val dateFormatters = listOf(
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
    )
}
