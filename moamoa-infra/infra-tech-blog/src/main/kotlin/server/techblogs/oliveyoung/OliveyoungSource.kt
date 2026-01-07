package server.techblogs.oliveyoung

import kotlinx.coroutines.flow.Flow
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import server.utill.PagingFinishedException
import server.utill.fetchWithPaging
import java.time.LocalDate
import java.time.LocalDateTime

@Component
internal class OliveyoungSource : TechBlogSource {

    private val baseUrl = "https://oliveyoung.tech"
    private val timeoutMs = 10_000

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        val seenKeys = HashSet<String>(4096)

        return fetchWithPaging(size, ::buildListUrl, timeoutMs = timeoutMs) { doc ->
            // 글 링크 패턴: "/YYYY-MM-DD/slug/"
            val linkEls = doc.select("a[href~=(?i)^/\\d{4}-\\d{2}-\\d{2}/.+/?$]")
            if (linkEls.isEmpty()) throw PagingFinishedException()

            val parsed = linkEls.mapNotNull { linkEl ->
                val href = linkEl.attr("href").trim()
                if (href.isBlank()) return@mapNotNull null

                val key = extractKeyFromHref(href)
                if (key.isBlank()) return@mapNotNull null

                val url = linkEl.absUrl("href")
                if (url.isBlank()) return@mapNotNull null

                // 카드 단위로 파싱하기 위해 부모 li를 잡음 (없으면 a 자체로 진행)
                val root: Element = linkEl.parents().firstOrNull { it.tagName().equals("li", ignoreCase = true) } ?: linkEl

                val title = root.selectFirst("h1")?.text()?.trim()
                    ?: return@mapNotNull null

                val description = root.selectFirst("p")?.text()?.trim().orEmpty()

                val dateText = root.select("span")
                    .asSequence()
                    .map { it.text().trim() }
                    .firstOrNull { it.matches(DATE_REGEX) }

                val publishedAt = dateText?.let { LocalDate.parse(it).atStartOfDay() } ?: LocalDateTime.MIN

                // category: "Tech", "Culture" (날짜가 아닌 span 중 첫 번째)
                val tag = root.select("span")
                    .asSequence()
                    .map { it.text().trim() }
                    .firstOrNull { it.isNotBlank() && !it.matches(DATE_REGEX) }
                    .orEmpty()

                // tags: TagList span들 (category/date 제외)
                val tags = root.select("span")
                    .asSequence()
                    .map { it.text().trim() }
                    .filter { it.isNotBlank() }
                    .filterNot { it == tag }
                    .filterNot { it == dateText }
                    .distinct()
                    .toList()

                val categories = (listOf(tag) + tags)
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()

                val thumbnail =
                    root.selectFirst("img[data-main-image]")
                        ?.absUrl("src")
                        ?.takeIf { it.isNotBlank() }
                        ?: root.selectFirst("img[srcset]")
                            ?.attr("srcset")
                            ?.split(',')
                            ?.firstOrNull()
                            ?.trim()
                            ?.substringBefore(' ')
                            ?.let { if (it.startsWith("/")) baseUrl + it else it }
                        ?: throw IllegalStateException("no thumbnail")

                TechBlogPost(
                    key = key,
                    title = title,
                    description = description,
                    tags = categories,
                    thumbnail = thumbnail,
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
        if (page == 1) baseUrl else "$baseUrl/page/$page/"

    private fun extractKeyFromHref(href: String): String {
        // "/2025-12-31/generics-and-parametric-polymorphism/" -> "2025-12-31/generics-and-parametric-polymorphism"
        val cleaned = href.substringBefore('?').substringBefore('#').trim()
        return cleaned.trim('/').trim()
    }

    companion object {
        private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    }
}