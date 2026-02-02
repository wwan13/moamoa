package server.techblogs.skplanet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import server.utill.jsoup
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
internal class SkplanetSource : TechBlogSource {

    private val baseUrl = "https://techtopic.skplanet.com"
    private val timeoutMs = 10_000

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> = flow {
        val doc = withContext(Dispatchers.IO) {
            jsoup(baseUrl, timeoutMs)
        }

        val posts = doc.select("article.post-list-item")
        if (posts.isEmpty()) return@flow

        val limit = size ?: posts.size
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

        posts.take(limit).forEach { post ->
            val linkEl = post.selectFirst("h2.title a[href]") ?: return@forEach

            val url = linkEl.absUrl("href").ifBlank {
                val href = linkEl.attr("href").trim()
                if (href.startsWith("/")) baseUrl + href else href
            }
            if (url.isBlank()) return@forEach

            val title = linkEl.text().trim()
            if (title.isBlank()) return@forEach

            val description = post.selectFirst("p[itemprop=description]")
                ?.text()
                ?.trim()
                .orEmpty()

            val dateText = post.selectFirst("small")
                ?.text()
                ?.trim()
                ?.let { DATE_REGEX.find(it)?.value }

            val publishedAt = dateText
                ?.let { runCatching { LocalDate.parse(it, formatter).atStartOfDay() }.getOrNull() }
                ?: LocalDateTime.MIN

            val tags = post.select("div.tags a span, div.tags a")
                .map { it.text().trim() }
                .filter { it.isNotBlank() }
                .distinct()

            emit(
                TechBlogPost(
                    key = extractKey(url),
                    title = title,
                    description = description,
                    tags = tags,
                    publishedAt = publishedAt,
                    url = url,
                    thumbnail = "https://i.imgur.com/yNgdwDC.png"
                )
            )
        }
    }

    private fun extractKey(url: String): String =
        URI(url).path.trimEnd('/').substringAfterLast('/')

    private companion object {
        private val DATE_REGEX = Regex("\\d{4}\\.\\d{2}\\.\\d{2}")
    }
}
