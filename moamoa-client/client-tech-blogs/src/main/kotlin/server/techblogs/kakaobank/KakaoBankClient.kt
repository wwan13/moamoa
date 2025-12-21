package server.techblogs.kakaobank

import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import server.client.techblogs.TechBlogClient
import server.client.techblogs.TechBlogPost
import server.utill.jsoup.HttpStatusException
import server.utill.jsoup.jsoup
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class KakaoBankClient : TechBlogClient {

    private val baseUrl: String = "https://tech.kakaobank.com"

    override fun getPosts(size: Int?): List<TechBlogPost> {
        val target = size ?: Int.MAX_VALUE
        val result = mutableListOf<TechBlogPost>()

        var page = 1
        while (result.size < target) {
            val url = if (page == 1) baseUrl else "$baseUrl/page/$page/"

            val doc = try {
                jsoup(url)
            } catch (e: HttpStatusException) {
                if (e.statusCode == 403) break
                throw e
            }

            val items = parseList(doc)
            if (items.isEmpty()) break

            result += items
            page++
        }

        return result.take(target)
    }

    private fun parseList(doc: Document): List<TechBlogPost> {
        val posts = doc.select("div.post")
        if (posts.isEmpty()) return emptyList()

        return posts.mapNotNull { post ->
            // 제목 + 링크
            val linkEl = post.selectFirst("h2.post-title > a")
                ?: return@mapNotNull null

            val url = linkEl.absUrl("href")
            if (url.isBlank()) return@mapNotNull null

            val title = linkEl.text().trim()
            if (title.isBlank()) return@mapNotNull null

            // description (목록 요약)
            val description = post.selectFirst("div.post-summary")
                ?.text()
                ?.trim()
                .orEmpty()

            // 날짜: 2025-12-04
            val publishedAt = post.selectFirst("div.post-info > div.date")
                ?.text()
                ?.trim()
                ?.let { LocalDate.parse(it).atStartOfDay() }
                ?: LocalDateTime.MIN

            // 카테고리 + 태그
            val categories =
                post.select("div.category a, div.sidebar-tags a")
                    .map { it.text().trim() }
                    .filter { it.isNotBlank() }
                    .distinct()

            TechBlogPost(
                key = extractKey(url),
                title = title,
                description = description,
                categories = categories,
                thumbnail = "",
                publishedAt = publishedAt,
                url = url
            )
        }
    }

    private fun extractKey(url: String): String =
        URI(url).path.trimEnd('/').substringAfterLast('/')
}