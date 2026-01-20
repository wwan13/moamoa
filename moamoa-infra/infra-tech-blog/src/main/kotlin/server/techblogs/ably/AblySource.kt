package server.techblogs.ably

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import java.time.LocalDate

@Component
internal class AblySource(
    private val webClient: WebClient,
) : TechBlogSource {

    private val baseUrl = "https://ably.team"
    private val category = "community"

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        val buildId = fetchBuildId()

        val response = webClient.get()
            .uri("$baseUrl/_next/data/$buildId/news.json?category=$category")
            .retrieve()
            .bodyToMono(AblyNewsResponse::class.java)
            .awaitSingle()

        val items = response.pageProps.communityArticles

        val flow = items.mapNotNull { toPost(it) }.asFlow()
        return if (size != null) flow.take(size) else flow
    }

    private suspend fun fetchBuildId(): String {
        val html = webClient.get()
            .uri("$baseUrl/news?category=$category")
            .retrieve()
            .bodyToMono(String::class.java)
            .awaitSingle()

        val match = BUILD_ID_REGEX.find(html)
            ?: throw IllegalStateException("Ably buildId 추출 실패")

        return match.groupValues[1]
    }

    private fun toPost(article: AblyCommunityArticle): TechBlogPost? {
        val fullUrl = "${baseUrl}/news/${article.id}"

        return TechBlogPost(
            key = article.id,
            title = article.title.firstOrNull()?.text ?: throw IllegalStateException("no title"),
            description = article.content?.firstOrNull { it.type == "paragraph" }?.text
                ?: "",
            tags = listOf(),
            thumbnail = article.representationImage?.url ?: throw IllegalStateException("no image"),
            publishedAt = LocalDate.parse(article.createdAt).atStartOfDay(),
            url = fullUrl
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AblyNewsResponse(
        val pageProps: PageProps,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class PageProps(
        val communityArticles: List<AblyCommunityArticle>,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AblyCommunityArticle(
        val id: String,
        val slug: String,
        val title: List<RichText>,
        val createdAt: String,
        val representationImage: RepresentationImage?,
        val content: List<RichText>?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class RichText(
        val type: String?,
        val text: String?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class RepresentationImage(
        val url: String?,
        val dimensions: Dimensions?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Dimensions(
        val width: Int?,
        val height: Int?,
    )

    companion object {
        private val BUILD_ID_REGEX = Regex(""""buildId"\s*:\s*"([^"]+)"""")
    }
}
