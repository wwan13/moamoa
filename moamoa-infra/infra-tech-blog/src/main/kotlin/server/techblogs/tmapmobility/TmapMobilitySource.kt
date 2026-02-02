package server.techblogs.tmapmobility

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
import java.time.ZoneId

@Component
internal class TmapMobilitySource(
    private val webClient: WebClient,
) : TechBlogSource {

    private val zoneId = ZoneId.of("Asia/Seoul")

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        var lastTime = 0L

        return fetchWithPaging(pageSize = PAGE_SIZE, targetCount = size) { _, _ ->
            val response = webClient.get()
                .uri {
                    it.scheme(SCHEME)
                        .host(HOST)
                        .path(PATH)
                        .queryParam("lastTime", lastTime)
                        .queryParam("thumbnail", "Y")
                        .queryParam("membershipContent", "false")
                        .build()
                }
                .retrieve()
                .handlePagingFinished()
                .bodyToMono(ApiResponse::class.java)
                .awaitSingle()

            val items = response.data?.list.orEmpty()
            items.validateIsPagingFinished()

            val nextLastTime = items.lastOrNull()?.publishTimestamp
                ?: items.lastOrNull()?.publishTime
                ?: lastTime

            if (nextLastTime == lastTime) {
                throw IllegalStateException("blogKey=$BLOG_KEY, url=$API_URL, field=lastTime")
            }
            lastTime = nextLastTime

            items.map { item ->
                val title = requireField(item.title, "title", item)
                val key = extractKey(item)
                val url = buildArticleUrl(item)
                val publishedAt = item.publishTimestamp
                    ?: item.publishTime
                    ?: 0L

                val thumbnail = item.articleImageForHomeOrDefault
                    ?: item.articleImageForHome
                    ?: item.articleImageList
                        ?.firstOrNull { it.type == "cover" }
                        ?.url
                    ?: DEFAULT_THUMBNAIL

                TechBlogPost(
                    key = key,
                    title = title,
                    description = item.contentSummary?.trim().orEmpty(),
                    tags = parseTags(item.articleKeywordNameAsCsv),
                    thumbnail = requireField(thumbnail, "thumbnail", item),
                    publishedAt = epochMsToLocalDateTime(publishedAt),
                    url = url
                )
            }
        }
    }

    private fun buildArticleUrl(item: ArticleItem): String {
        val profileId = item.profileId?.trim().orEmpty()
        val articleNo = item.no
        if (profileId.isBlank() || articleNo == null) {
            throw IllegalStateException("blogKey=$BLOG_KEY, url=$API_URL, field=url")
        }
        return "https://brunch.co.kr/@$profileId/$articleNo"
    }

    private fun extractKey(item: ArticleItem): String {
        val contentId = item.contentId?.trim().orEmpty()
        if (contentId.isNotBlank()) return contentId

        val articleNo = item.no?.toString().orEmpty()
        if (articleNo.isBlank()) {
            throw IllegalStateException("blogKey=$BLOG_KEY, url=$API_URL, field=key")
        }
        return articleNo
    }

    private fun parseTags(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(',')
            .map { it.trim().normalizeTagTitle() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun epochMsToLocalDateTime(epochMs: Long): LocalDateTime {
        if (epochMs <= 0L) return LocalDateTime.MIN
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), zoneId)
    }

    private fun requireField(value: String?, field: String, item: ArticleItem): String {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isBlank()) {
            val articleNo = item.no?.toString().orEmpty().ifBlank { "unknown" }
            throw IllegalStateException("blogKey=$BLOG_KEY, url=$API_URL, field=$field, articleNo=$articleNo")
        }
        return trimmed
    }

    private data class ApiResponse(
        val data: Data?
    ) {
        data class Data(
            val list: List<ArticleItem>?
        )
    }

    private data class ArticleItem(
        val no: Long?,
        val title: String?,
        val contentSummary: String?,
        val profileId: String?,
        val contentId: String?,
        val publishTime: Long?,
        val publishTimestamp: Long?,
        val articleImageForHome: String?,
        val articleImageForHomeOrDefault: String?,
        val articleImageList: List<ArticleImage>?,
        val articleKeywordNameAsCsv: String?
    ) {
        data class ArticleImage(
            val type: String?,
            val url: String?
        )
    }

    companion object {
        private const val BLOG_KEY = "tmapmobility"
        private const val SCHEME = "https"
        private const val HOST = "api.brunch.co.kr"
        private const val PATH = "/v2/article/@tmapmobility"
        private const val API_URL = "https://api.brunch.co.kr/v2/article/@tmapmobility"
        private const val PAGE_SIZE = 20
        private const val DEFAULT_THUMBNAIL = ""
    }
}
