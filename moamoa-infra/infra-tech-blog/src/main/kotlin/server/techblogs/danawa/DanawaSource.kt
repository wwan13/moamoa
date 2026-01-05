package server.techblogs.danawa

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
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
class DanawaSource : TechBlogSource {

    private val baseUrl = "https://danawalab.github.io"
    private val timeoutMs = 10_000

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        val tags = fetchTags()

        return tags
            .asFlow()
            .flatMapMerge(concurrency = 6) { tag ->
                fetchPostsByTag(tag)
            }
    }

    private suspend fun fetchTags(): List<Tag> = withContext(Dispatchers.IO) {
        val doc = jsoup(baseUrl, timeoutMs)

        doc.select("ul li a[href^=/category/]")
            .mapNotNull { a ->
                val href = a.attr("href")
                val name = a.text()
                    .substringBefore("(")
                    .trim()
                    .lowercase()

                if (href.isBlank() || name.isBlank()) return@mapNotNull null

                Tag(
                    name = name,
                    url = baseUrl + href
                )
            }
            .distinctBy { it.url }
    }

    private fun fetchPostsByTag(tag: Tag): Flow<TechBlogPost> = flow {
        val doc = jsoup(tag.url, timeoutMs)

        val posts = doc.select("div.content__post")
        for (post in posts) {
            val link = post.selectFirst("a.content__link[href]") ?: continue
            val url = baseUrl + link.attr("href")

            val title = post.selectFirst("h3.content__h3")
                ?.text()
                ?.trim()
                ?: continue

            val description = post.selectFirst("p.content__p")
                ?.text()
                ?.trim()
                .orEmpty()

            val publishedAtRaw = post.selectFirst("span.date")
                ?.text()
                ?.trim()
                ?.trimEnd('.')
                ?: continue

            emit(
                TechBlogPost(
                    key = extractKey(url),
                    title = title,
                    description = description,
                    tags = listOf(tag.name),
                    thumbnail = "https://i.imgur.com/KxL8Fxu.png",
                    publishedAt = parseDate(publishedAtRaw),
                    url = url
                )
            )
        }
    }

    /* ================= util ================= */

    private fun extractKey(url: String): String =
        URI(url).path.trimEnd('/')

    private fun parseDate(raw: String): LocalDateTime =
        LocalDate.parse(raw, formatter).atStartOfDay()

    private val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    private data class Tag(
        val name: String,
        val url: String
    )
}