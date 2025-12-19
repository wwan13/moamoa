package server.toss

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import server.client.techblogs.TechBlogClient
import server.client.techblogs.TechBlogPost
import server.paging.fetchWithPaging
import server.paging.handlePagingFinished
import server.paging.validateIsPagingFinished
import java.time.ZonedDateTime

@Component
class TossClient(
    private val webClient: WebClient
) : TechBlogClient {

    override fun getPosts(size: Int?): List<TechBlogPost> {
        return fetchWithPaging(pageSize = 100, targetCount = size) { size, page ->
            val response = webClient.get()
                .uri {
                    it
                        .scheme(SCHEME)
                        .host(HOST)
                        .path(PATH)
                        .queryParam(SIZE, size)
                        .queryParam(PAGE, page)
                        .build()
                }
                .retrieve()
                .handlePagingFinished()
                .onStatus({ it.isError }) { res ->
                    res.bodyToMono(String::class.java).defaultIfEmpty("").flatMap { body ->
                        Mono.error(IllegalStateException("toss api error: status=${res.statusCode()} body=$body"))
                    }
                }
                .bodyToMono(TossPostResponse::class.java)
                .block()
                ?: throw IllegalStateException("toss response is null")

            if (response.success == null || response.resultType != SUCCESS_FLAG) {
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
                    url = POST_BASE_URL + it.key
                )
            }
        }
    }

    companion object {
        private const val SCHEME = "https"
        private const val HOST = "api-public.toss.im"
        private const val PATH = "/api-public/v3/ipd-thor/api/v1/workspaces/15/posts"
        private const val SIZE = "size"
        private const val PAGE = "page"
        private const val POST_BASE_URL = "https://toss.tech/article/"
        private const val SUCCESS_FLAG = "SUCCESS"
    }
}