package server.techblogs.postype

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import server.utill.fetchWithPaging
import server.utill.handlePagingFinished
import server.utill.normalizeTagTitle
import server.utill.validateIsPagingFinished
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
internal class PostypeSource(
    private val webClient: WebClient
) : TechBlogSource {

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        return fetchWithPaging(pageSize = PAGE_SIZE, targetCount = size, startPage = START_PAGE) { pageSize, page ->
            val response = webClient.get()
                .uri { it.scheme(SCHEME).host(HOST).path(PATH)
                    .queryParam("page", page)
                    .queryParam("size", pageSize)
                    .queryParam("sortType", SORT_TYPE)
                    .build()
                }
                .retrieve()
                .handlePagingFinished()
                .bodyToMono(PostypeResponse::class.java)
                .awaitSingle()

            val items = response.content.orEmpty()
            items.validateIsPagingFinished()

            items.mapNotNull { wrapper ->
                val item = wrapper.feedItem ?: return@mapNotNull null

                val url = requireField(item.shortUrl, "url", null)
                val key = requireField(item.postId?.toString(), "key", url)
                val title = requireField(item.title, "title", url)

                val thumbnail = item.thumbnails?.firstOrNull()?.imagePath
                    ?: "https://i.imgur.com/4kBg9Fl.png"

                val publishedAt = item.publishedAt
                    ?.let { Instant.ofEpochSecond(it).atZone(ZoneOffset.UTC).toLocalDateTime() }
                    ?: LocalDateTime.MIN

                val description = item.summary?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: item.subTitle?.trim().orEmpty()

                val tags = item.tags
                    ?.mapNotNull { it?.normalizeTagTitle()?.ifBlank { null } }
                    ?.distinct()
                    .orEmpty()

                TechBlogPost(
                    key = key,
                    title = title,
                    description = description,
                    tags = tags,
                    thumbnail = thumbnail,
                    publishedAt = publishedAt,
                    url = url
                )
            }
        }
    }

    private fun requireField(value: String?, field: String, url: String?): String {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isBlank()) {
            val urlValue = url?.takeIf { it.isNotBlank() } ?: "unknown"
            throw IllegalStateException("blogKey=$BLOG_KEY, url=$urlValue, field=$field")
        }
        return trimmed
    }

    private data class PostypeResponse(
        val content: List<ContentWrapper>?
    ) {
        data class ContentWrapper(
            val type: String?,
            val feedItem: FeedItem?
        )

        data class FeedItem(
            val postId: Long?,
            val title: String?,
            val subTitle: String?,
            val summary: String?,
            val thumbnails: List<Thumbnail>?,
            val tags: List<String?>?,
            val shortUrl: String?,
            val publishedAt: Long?
        ) {
            data class Thumbnail(
                val imagePath: String?
            )
        }
    }

    companion object {
        private const val BLOG_KEY = "postype"
        private const val SCHEME = "https"
        private const val HOST = "api.postype.com"
        private const val PATH = "/api/v1/channel/516863/activity/posts"
        private const val SORT_TYPE = "RECENT"
        private const val PAGE_SIZE = 12
        private const val START_PAGE = 0
    }
}
