package server.techblogs.kakaobank

import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import server.utill.jsoup.fetchWithPaging
import server.utill.webclient.PagingFinishedException
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class KakaoBankSource : TechBlogSource {

    private val baseUrl: String = "https://tech.kakaobank.com"

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        return fetchWithPaging(size, ::buildUrl) { doc ->
            val posts = doc.select("div.post")
            if (posts.isEmpty()) throw PagingFinishedException()

            posts.mapNotNull { post ->
                val linkEl = post.selectFirst("h2.post-title > a") ?: return@mapNotNull null

                val url = linkEl.absUrl("href")
                if (url.isBlank()) return@mapNotNull null

                val title = linkEl.text().trim()
                if (title.isBlank()) return@mapNotNull null

                val description = post.selectFirst("div.post-summary")
                    ?.text()
                    ?.trim()
                    .orEmpty()

                val publishedAt = post.selectFirst("div.post-info > div.date")
                    ?.text()
                    ?.trim()
                    ?.let { LocalDate.parse(it).atStartOfDay() }
                    ?: LocalDateTime.MIN

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
                    publishedAt = publishedAt,
                    url = url
                )
            }
        }
    }

    private fun buildUrl(page: Int) = if (page == 1) baseUrl else "$baseUrl/page/$page/"

    private fun extractKey(url: String): String =
        URI(url).path.trimEnd('/').substringAfterLast('/')
}