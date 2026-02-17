package server.techblogs.flex

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
internal class FlexSource : TechBlogSource {

    private val baseUrl = "https://flex.team/blog/category/flexteam/"
    private val timeoutMs = 10_000

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        val seenKeys = HashSet<String>(2048)

        return fetchWithPaging(targetCount = size, buildUrl = ::buildListUrl, timeoutMs = timeoutMs) { doc ->
            // ✅ 글 상세 링크만 (카테고리/태그/블로그 홈 링크 제외)
            val postLinks = doc.select("a[href^=/blog/]")
                .asSequence()
                .filterNot { el ->
                    val href = el.attr("href")
                    href == "/blog/" ||
                            href.startsWith("/blog/category/") ||
                            href.startsWith("/blog/tag/") ||
                            href.startsWith("/blog/search")
                }
                .filter { it.text().trim().isNotBlank() }
                .toList()

            if (postLinks.isEmpty()) throw PagingFinishedException()

            val parsed = postLinks.mapNotNull { linkEl ->
                val url = requireField(linkEl.absUrl("href"), "url", null)
                val key = requireField(extractKey(url), "key", url)

                // ✅ 카드 범위: 링크 기준으로 가장 가까운 li (없으면 부모)
                val card = linkEl.closest("li")
                    ?: linkEl.parent()
                    ?: throw IllegalStateException("blogKey=$BLOG_KEY, url=$url, field=card")

                val title = requireField(linkEl.text(), "title", url)

                val description = extractDescription(card, title)

                val tags = card.select("a[href^=/blog/category/]")
                    .mapNotNull { it.text().trim().ifBlank { null } }
                    .distinct()

                val publishedAt = extractPublishedAt(card.text())

                val thumbnail = requireField(extractThumbnail(card), "thumbnail", url)

                TechBlogPost(
                    key = key,
                    title = title,
                    description = description,
                    tags = tags,
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
        if (page == 1) baseUrl else throw PagingFinishedException()

    private fun extractKey(url: String): String =
        URI(url).path.trimEnd('/').substringAfterLast('/')

    private fun extractPublishedAt(cardText: String): LocalDateTime {
        val raw = dateRegex.find(cardText)?.value
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?: return LocalDateTime.MIN

        return runCatching { LocalDate.parse(raw, DATE_FORMATTER).atStartOfDay() }
            .getOrDefault(LocalDateTime.MIN)
    }

    private fun extractThumbnail(card: org.jsoup.nodes.Element): String? {
        val img = card.selectFirst("img[src]") ?: return null
        val abs = img.absUrl("src").trim()
        return abs.ifBlank { img.attr("src").trim().takeIf { it.isNotBlank() } }
    }

    private fun extractDescription(card: org.jsoup.nodes.Element, title: String): String {
        // ✅ 너무 짧은 라벨(뉴스/팀 스토리 등) 제외하고, 본문에 가까운 텍스트를 추출
        val candidates = card.select("p, div")
            .asSequence()
            .map { it.text().trim() }
            .filter { it.isNotBlank() }
            .filterNot { it == title }
            .filterNot { dateRegex.containsMatchIn(it) }
            .filterNot { it.length <= 6 } // "뉴스", "팀 스토리" 같은 것들 제거
            .toList()

        return candidates.firstOrNull().orEmpty()
    }

    private fun requireField(value: String?, field: String, url: String?): String {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isBlank()) {
            val urlValue = url?.takeIf { it.isNotBlank() } ?: "unknown"
            throw IllegalStateException("blogKey=$BLOG_KEY, url=$urlValue, field=$field")
        }
        return trimmed
    }

    companion object {
        private const val BLOG_KEY = "flex"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy. M. d")
        private val dateRegex = Regex("""\d{4}\.\s*\d{1,2}\.\s*\d{1,2}""")
    }
}
