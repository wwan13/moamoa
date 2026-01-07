package server.techblogs.nhncloud

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import server.utill.PagingFinishedException
import server.utill.fetchWithPaging
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
internal class NhnCloudSource(
    private val webClient: WebClient
) : TechBlogSource {

    private val scheme = "https"
    private val host = "meetup.nhncloud.com"
    private val apiPath = "/tcblog/v1.0/posts"
    private val postBaseUrl = "https://meetup.nhncloud.com/posts/"
    private val pageSize = 12

    // 예: 2025-11-12T08:35:19.000+0900
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        return fetchWithPaging(pageSize = pageSize, targetCount = size) { rowsPerPage, pageNo ->
            val response = webClient.get()
                .uri {
                    it.scheme(scheme).host(host).path(apiPath)
                        .queryParam("pageNo", pageNo)
                        .queryParam("rowsPerPage", rowsPerPage)
                        .build()
                }
                .retrieve()
                .bodyToMono(Response::class.java)
                .awaitSingle()

            if (response.header?.isSuccessful != true) {
                throw IllegalStateException("nhnCloud meetup api 호출에 실패했습니다.")
            }

            val posts = response.posts.orEmpty()
            if (posts.isEmpty()) throw PagingFinishedException()

            posts.mapNotNull { p ->
                val lang = p.postPerLang ?: return@mapNotNull null

                val title = lang.title?.trim().orEmpty()
                if (title.isBlank()) return@mapNotNull null

                val description = lang.description?.trim().orEmpty()

                val thumbnail = lang.repImageUrl?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: "https://i.imgur.com/p6VD51o.png"

                val publishedAt = parseDateTime(p.publishTime ?: p.regTime)

                TechBlogPost(
                    key = p.postId.toString(),
                    title = title,
                    description = description,
                    tags = emptyList(), // 요청대로 비움
                    thumbnail = thumbnail,
                    publishedAt = publishedAt,
                    url = postBaseUrl + p.postId
                )
            }
        }
    }

    private fun parseDateTime(raw: String?): LocalDateTime {
        if (raw.isNullOrBlank()) return LocalDateTime.MIN
        return runCatching { ZonedDateTime.parse(raw.trim(), timeFormatter).toLocalDateTime() }
            .getOrDefault(LocalDateTime.MIN)
    }

    private data class Response(
        val header: Header?,
        val totalCount: Int?,
        val posts: List<Post>?
    ) {
        data class Header(
            val isSuccessful: Boolean?,
            val resultCode: Int?,
            val resultMessage: String?
        )

        data class Post(
            val postId: Long,
            val regTime: String?,
            val publishTime: String?,
            val postPerLang: PostPerLang?
        ) {
            data class PostPerLang(
                val title: String?,
                val description: String?,
                val repImageUrl: String?
            )
        }
    }
}