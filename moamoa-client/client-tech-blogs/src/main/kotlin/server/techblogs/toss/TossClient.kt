package server.techblogs.toss

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import server.client.techblogs.TechBlogClient
import server.client.techblogs.TechBlogPost
import server.utill.webclient.fetchWithPaging
import server.utill.webclient.handlePagingFinished
import server.utill.webclient.validateIsPagingFinished
import java.time.ZonedDateTime

@Component
class TossClient(
    private val webClient: WebClient
) : TechBlogClient {

    private val scheme = "https"
    private val host = "api-public.toss.im"
    private val path = "/api-public/v3/ipd-thor/api/v1/workspaces/15/posts"
    private val postBaseUrl = "https://toss.tech/article/"

    override fun getPosts(size: Int?): List<TechBlogPost> {
        return fetchWithPaging(pageSize = 100, targetCount = size) { size, page ->
            val response = webClient.get()
                .uri {
                    it
                        .scheme(scheme)
                        .host(host)
                        .path(path)
                        .queryParam("size", size)
                        .queryParam("page", page)
                        .build()
                }
                .retrieve()
                .handlePagingFinished()
                .onStatus({ it.isError }) { res ->
                    res.bodyToMono(String::class.java).defaultIfEmpty("").flatMap { body ->
                        Mono.error(IllegalStateException("toss api error: status=${res.statusCode()} body=$body"))
                    }
                }
                .bodyToMono(Response::class.java)
                .block()
                ?: throw IllegalStateException("toss response is null")

            if (response.success == null || response.resultType != "SUCCESS") {
                throw IllegalStateException("toss tech blog 정보를 불러오는데 실패하였습니다.")
            }
            response.success.results.validateIsPagingFinished()

            response.success.results.map {
                TechBlogPost(
                    key = it.key,
                    title = it.title,
                    description = it.subtitle,
                    categories = listOf(it.category),
                    thumbnail = it.thumbnailConfig.imageUrl,
                    publishedAt = ZonedDateTime.parse(it.publishedTime).toLocalDateTime(),
                    url = postBaseUrl + it.key
                )
            }
        }
    }

    private data class Response(
        val resultType: String,
        val success: Success?
    ) {
        data class Success(
            val page: Int,
            val pageSize: Int,
            val count: Int,
            val next: String?,
            val previous: String?,
            val results: List<Post>
        ) {
            data class Post(
                val id: Long,
                val key: String,
                val title: String,
                val subtitle: String,
                val category: String,
                val publishedTime: String,
                val thumbnailConfig: ThumbnailConfig,
            ) {
                data class ThumbnailConfig(
                    val imageUrl: String
                )
            }
        }
    }
}