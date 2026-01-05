package server.techblogs.gabia

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import server.utill.fetchWithPaging
import server.utill.handlePagingFinished
import server.utill.validateIsPagingFinished
import java.time.LocalDateTime

@Component
class GabiaSource(
    private val webClient: WebClient
) : TechBlogSource {

    private val baseUrl = "https://library.gabia.com"
    private val apiPath = "/wp-json/wp/v2/posts"

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        return fetchWithPaging(pageSize = 10, targetCount = size) { pageSize, page ->
            val posts = webClient.get()
                .uri {
                    it.scheme("https")
                        .host("library.gabia.com")
                        .path(apiPath)
                        .queryParam("page", page)
                        .queryParam("per_page", pageSize)
                        .queryParam("_embed", "1") // 썸네일 포함
                        .build()
                }
                .retrieve()
                .handlePagingFinished()
                .bodyToMono(Array<WpPost>::class.java)
                .awaitSingle()
                .toList()

            posts.validateIsPagingFinished()

            posts.map {
                TechBlogPost(
                    key = it.id.toString(),
                    title = it.title.rendered.cleanHtml(),
                    description = it.excerpt?.rendered?.cleanHtml().orEmpty(),
                    tags = emptyList(), // 태그 필요하면 wp:term에서 추가
                    thumbnail = it.thumbnailUrl ?: DEFAULT_THUMBNAIL,
                    publishedAt = LocalDateTime.parse(it.date),
                    url = it.link
                )
            }
        }
    }

    private data class WpPost(
        val id: Long,
        val date: String,
        val link: String,
        val title: Rendered,
        val excerpt: Rendered?,
        val _embedded: Embedded?
    ) {
        val thumbnailUrl: String?
            get() = _embedded?.featuredMedia?.firstOrNull()?.sourceUrl

        data class Rendered(
            val rendered: String
        )

        data class Embedded(
            @JsonProperty("wp:featuredmedia")
            val featuredMedia: List<FeaturedMedia>?
        )

        data class FeaturedMedia(
            @JsonProperty("source_url")
            val sourceUrl: String?
        )
    }

    private fun String.cleanHtml(): String =
        replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .trim()

    companion object {
        private const val DEFAULT_THUMBNAIL = "https://i.imgur.com/LZiefd9.png"
    }
}