package server.techblogs.woowabros

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import server.client.techblogs.TechBlogClient
import server.client.techblogs.TechBlogPost
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class WoowabrosClient : TechBlogClient {

    private val baseUrl = "https://techblog.woowahan.com"
    private val timeoutMs = 10_000

    override fun getPosts(size: Int?): List<TechBlogPost> {
        val target = size ?: Int.MAX_VALUE

        // 1) 목록에서 기본 필드 수집
        val indexed = fetchFromList(target)

        // 2) 상세에서 categories만 채우기
        return indexed.map { base ->
            val categories = runCatching { fetchCategories(base.url) }.getOrDefault(emptyList())
            base.copy(categories = categories)
        }
    }

    private fun fetchFromList(target: Int): List<TechBlogPost> {
        val acc = mutableListOf<TechBlogPost>()
        var page = 1

        while (acc.size < target) {
            val listUrl = if (page == 1) baseUrl else "$baseUrl/?paged=$page"
            val doc = get(listUrl)

            val items = parseList(doc)
            if (items.isEmpty()) break

            acc += items
            page++
        }

        return acc.take(target)
    }

    private fun parseList(doc: Document): List<TechBlogPost> {
        val items = doc.select("div.post-item")
        if (items.isEmpty()) return emptyList()

        return items.mapNotNull { item ->
            // 글 링크 (h2, p를 감싸는 a)
            val linkEl = item.selectFirst("a[href]:has(h2.post-title)")
                ?: return@mapNotNull null

            val url = linkEl.absUrl("href")
            if (url.isBlank()) return@mapNotNull null

            val title = linkEl.selectFirst("h2.post-title")
                ?.text()
                ?.trim()
                ?: return@mapNotNull null

            // ✅ 네가 원한 description (목록에 보이는 요약)
            val description = linkEl.selectFirst("p.post-excerpt")
                ?.text()
                ?.trim()
                .orEmpty()

            // 날짜: "2025. 12. 10."
            val publishedAt = item.selectFirst("time.post-author-date")
                ?.text()
                ?.let { parseDateTime(it) }
                ?: LocalDateTime.MIN

            TechBlogPost(
                key = extractKey(url),
                title = title,
                description = description,
                categories = emptyList(),   // 상세에서 채움
                thumbnail = "",
                publishedAt = publishedAt,
                url = url
            )
        }
    }

    private fun fetchCategories(postUrl: String): List<String> {
        val doc = get(postUrl)

        return doc.select("span.cats a.cat-tag")
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

    private fun extractKey(url: String): String {
        val path = URI(url).path.trimEnd('/')
        return path.substringAfterLast('/')
    }

    private fun parseDateTime(raw: String): LocalDateTime  {
        val text = raw.trim()

        for (formatter in DATE_FORMATTERS) {
            runCatching {
                return LocalDate.parse(text, formatter).atStartOfDay()
            }
        }

        throw IllegalArgumentException("Unsupported date format: $raw")
    }

    private val DATE_FORMATTERS = listOf(
        DateTimeFormatter.ofPattern("yyyy. MM. dd."),
        DateTimeFormatter.ofPattern("MMM.dd.yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMM. dd. yyyy", Locale.ENGLISH),
    )
}