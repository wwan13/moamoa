package server.techblog.http

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import server.techblog.TechBlogPost

internal class TechBlogCrawlerClient(
    @param:Qualifier("techBlogCrawlerWebClient")
    private val webClient: WebClient,
) {

    suspend fun crawl(key: String, size: Int?): TechBlogCrawlerResult {
        val response = webClient.post()
            .uri {
                val builder = it.path("/crawl")
                    .queryParam("key", key)
                if (size != null) {
                    builder.queryParam("size", size)
                }
                builder.build()
            }
            .retrieve()
            .bodyToMono(CrawlResponse::class.java)
            .awaitSingle()

        return TechBlogCrawlerResult(
            key = response.key,
            posts = response.posts.map { it.toTechBlogPost() }
        )
    }

    private fun CrawlPostResponse.toTechBlogPost(): TechBlogPost {
        return TechBlogPost(
            key = key,
            title = title,
            description = description,
            tags = tags,
            thumbnail = thumbnail,
            publishedAt = publishedAt.toLocalDateTime(),
            url = url,
        )
    }

    private fun String.toLocalDateTime(): LocalDateTime {
        val value = trim()
        return runCatching { OffsetDateTime.parse(value).toLocalDateTime() }
            .recoverCatching { ZonedDateTime.parse(value).toLocalDateTime() }
            .recoverCatching { LocalDateTime.parse(value) }
            .recoverCatching { LocalDate.parse(value).atStartOfDay() }
            .recoverCatching { LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) }
            .getOrThrow()
    }

    private data class CrawlResponse(
        val key: String,
        val posts: List<CrawlPostResponse>,
    )

    private data class CrawlPostResponse(
        val key: String,
        val title: String,
        val description: String,
        val tags: List<String>,
        val thumbnail: String,
        val publishedAt: String,
        val url: String,
    )
}

internal data class TechBlogCrawlerResult(
    val key: String,
    val posts: List<TechBlogPost>,
)
