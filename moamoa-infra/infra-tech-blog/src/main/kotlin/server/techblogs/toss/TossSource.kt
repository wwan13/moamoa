package server.techblogs.toss

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import server.utill.fetchWithPaging
import server.utill.handlePagingFinished
import server.utill.validateIsPagingFinished
import java.time.ZonedDateTime

@Component
internal class TossSource(
    private val webClient: WebClient
) : TechBlogSource {

    private val scheme = "https"
    private val host = "api-public.toss.im"
    private val path = "/api-public/v3/ipd-thor/api/v1/workspaces/15/posts"
    private val postBaseUrl = "https://toss.tech/article/"

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        return fetchWithPaging(pageSize = 100, targetCount = size) { pageSize, page ->
            val response = webClient.get()
                .uri { it.scheme(scheme).host(host).path(path)
                    .queryParam("size", pageSize)
                    .queryParam("page", page)
                    .build()
                }
                .retrieve()
                .handlePagingFinished()
                .bodyToMono(Response::class.java)
                .awaitSingle()

            if (response.success == null || response.resultType != "SUCCESS") {
                throw IllegalStateException("toss tech blog 정보를 불러오는데 실패하였습니다.")
            }
            response.success.results.validateIsPagingFinished()

            response.success.results.map {
                TechBlogPost(
                    key = it.key,
                    title = it.title,
                    description = it.subtitle,
                    tags = listOf(it.category),
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