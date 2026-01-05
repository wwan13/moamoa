package server.techblogs.naver

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.awaitSingle
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import server.utill.fetchWithPaging
import server.utill.handlePagingFinished
import server.utill.normalizeTagTitle
import server.utill.validateIsPagingFinished
import java.net.URI
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class NaverSource(
    private val webClient: WebClient
) : TechBlogSource {

    private val scheme = "https"
    private val host = "d2.naver.com"
    private val baseUrl = "https://d2.naver.com"
    private val listPath = "/api/v1/contents"
    private val detailPathPrefix = "/api/v1/contents/" // + {key}
    private val zoneId = ZoneId.of("Asia/Seoul")

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        val listFlow: Flow<TechBlogPost> = fetchWithPaging(pageSize = 20, targetCount = size, startPage = 0) { pageSize, page ->
            val response = webClient.get()
                .uri {
                    it.scheme(scheme)
                        .host(host)
                        .path(listPath)
                        .queryParam("categoryId", "")
                        .queryParam("page", page)
                        .queryParam("size", pageSize)
                        .build()
                }
                .retrieve()
                .handlePagingFinished()
                .bodyToMono(ListResponse::class.java)
                .awaitSingle()

            response.content.validateIsPagingFinished()

            response.content.mapNotNull { item ->
                val relativeUrl = item.url?.trim().orEmpty()
                if (!relativeUrl.startsWith("/")) return@mapNotNull null

                val absoluteUrl = baseUrl + relativeUrl
                val key = extractKey(relativeUrl)
                if (key.isBlank()) return@mapNotNull null

                val title = item.postTitle?.trim().orEmpty()
                if (title.isBlank()) return@mapNotNull null

                val description = item.postHtml
                    ?.let { Jsoup.parse(it).text().trim() }
                    .orEmpty()

                val publishedAt = item.postPublishedAt
                    ?.let { epochMsToLocalDateTime(it) }
                    ?: LocalDateTime.MIN

                val thumbnail = item.postImage
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { if (it.startsWith("http")) it else baseUrl + it }
                    ?: DEFAULT_THUMBNAIL

                TechBlogPost(
                    key = key,
                    title = title,
                    description = description,
                    tags = emptyList(), // 상세에서 채움
                    thumbnail = thumbnail,
                    publishedAt = publishedAt,
                    url = absoluteUrl
                )
            }
        }

        return listFlow.flatMapMerge(concurrency = 10) { base ->
            flow {
                val detail = runCatching { fetchDetail(base.key) }.getOrNull()

                val categories = buildList {
                    detail?.categoryName?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
                    detail?.postTags.orEmpty()
                        .mapNotNull { it.name?.trim() }
                        .filter { it.isNotBlank() }
                        .forEach { add(it) }
                }
                    .map { it.normalizeTagTitle() }
                    .filter { it.isNotBlank() }
                    .distinct()

                emit(
                    base.copy(
                        tags = categories
                    )
                )
            }
        }
    }

    private suspend fun fetchDetail(key: String): DetailResponse {
        return webClient.get()
            .uri {
                it.scheme(scheme)
                    .host(host)
                    .path(detailPathPrefix + key)
                    .build()
            }
            .retrieve()
            .handlePagingFinished()
            .bodyToMono(DetailResponse::class.java)
            .awaitSingle()
    }

    private fun extractKey(relativeUrl: String): String =
        runCatching { URI(relativeUrl).path.trimEnd('/').substringAfterLast('/') }.getOrDefault("")

    private fun epochMsToLocalDateTime(epochMs: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), zoneId)

    private data class ListResponse(
        val content: List<Item>
    ) {
        data class Item(
            val postTitle: String?,
            val postImage: String?,
            val postHtml: String?,
            val postPublishedAt: Long?,
            val url: String?
        )
    }

    private data class DetailResponse(
        val categoryName: String?,
        val postTags: List<Tag>?
    ) {
        data class Tag(
            val name: String?
        )
    }

    companion object {
        private const val DEFAULT_THUMBNAIL = "https://i.imgur.com/FOrickO.png"
    }
}