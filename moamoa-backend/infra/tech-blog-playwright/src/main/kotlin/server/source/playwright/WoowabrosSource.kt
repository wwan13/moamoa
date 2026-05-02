package server.source.playwright

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.WaitUntilState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import server.config.PlaywrightProperties
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
internal class WoowabrosSource(
    private val playwrightProperties: PlaywrightProperties = PlaywrightProperties(),
) : TechBlogSource {

    private val objectMapper = ObjectMapper()

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> = flow {
        val posts = withContext(Dispatchers.IO) {
            scrapePosts(size)
        }

        posts.forEach { emit(it) }
    }

    internal fun parsePage(body: String): WoowabrosPage {
        val root = objectMapper.readTree(body)
        require(root.path("success").asBoolean()) { "Woowabros AJAX response was not successful." }

        val data = root.path("data")
        val posts = data.path("posts")
            .mapNotNull { node -> runCatching { parseSummary(node) }.getOrNull() }
            .distinctBy { it.url }

        val maxPage = data.path("pagination").path("max").asInt(0)
        return WoowabrosPage(posts = posts, maxPage = maxPage)
    }

    private fun scrapePosts(size: Int?): List<TechBlogPost> {
        Playwright.create().use { playwright ->
            openBrowser(playwright).use { browser ->
                browser.newContext(
                    Browser.NewContextOptions()
                        .setUserAgent(USER_AGENT)
                        .setLocale("ko-KR")
                ).use { context ->
                    val page = context.newPage()
                    page.navigate(BASE_URL, Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED))

                    val summaries = fetchSummaries(page, size)
                    println(summaries.size)
                    return summaries.map { summary ->
                        summary.copy(tags = fetchCategories(context, summary.url))
                    }
                }
            }
        }
    }

    private fun openBrowser(playwright: Playwright): Browser {
        val endpoint = playwrightProperties.wsEndpoint?.trim()
        if (!endpoint.isNullOrBlank()) {
            return playwright.chromium().connect(
                endpoint,
                BrowserType.ConnectOptions()
                    .setTimeout(10_000.0)
            )
        }

        return playwright.chromium().launch(
            BrowserType.LaunchOptions()
                .setHeadless(true)
        )
    }

    private fun fetchSummaries(page: Page, size: Int?): List<TechBlogPost> {
        val posts = mutableListOf<TechBlogPost>()
        var pageNo = 1
        var maxPage = Int.MAX_VALUE
        var firstSignature: String? = null

        while ((size == null || posts.size < size) && pageNo <= maxPage) {
            val body = page.evaluate(
                """
                async (pageNo) => {
                  const params = new URLSearchParams();
                  params.append('action', 'get_posts_data');
                  params.append('data[post][post_status]', 'publish');
                  params.append('data[post][paged]', String(pageNo));
                  params.append('data[meta]', 'main');
                  const response = await fetch('/wp-admin/admin-ajax.php', {
                    method: 'POST',
                    credentials: 'include',
                    headers: {
                      'X-Requested-With': 'XMLHttpRequest',
                      'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                      'Accept': 'application/json, text/javascript, */*; q=0.01'
                    },
                    body: params.toString()
                  });
                  return await response.text();
                }
                """.trimIndent(),
                pageNo
            ) as String

            val pageResult = parsePage(body)
            if (pageResult.posts.isEmpty()) break

            maxPage = pageResult.maxPage.takeIf { it > 0 } ?: maxPage

            val signature = pageResult.posts.joinToString("|") { it.url }
            if (pageNo == 1) firstSignature = signature
            if (pageNo > 1 && firstSignature == signature) break

            val remaining = if (size == null) pageResult.posts.size else size - posts.size
            posts += pageResult.posts.take(remaining.coerceAtLeast(0))
            pageNo++
        }

        return posts
    }

    private fun fetchCategories(context: BrowserContext, postUrl: String): List<String> {
        val page = context.newPage()
        return try {
            page.navigate(postUrl, Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED))
            page.locator("span.cats a.cat-tag")
                .allTextContents()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        } finally {
            page.close()
        }
    }

    private fun parseSummary(node: JsonNode): TechBlogPost {
        val url = node.path("permalink").asText().trim()
            .ifBlank { throw IllegalArgumentException("post url is blank") }

        val title = node.path("post_title").asText().trim()
            .takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("post title is blank")

        return TechBlogPost(
            key = extractKey(url),
            title = title,
            description = node.path("excerpt").asText().trim(),
            tags = emptyList(),
            thumbnail = "",
            publishedAt = parseDateTime(node.path("date").asText()),
            url = url,
        )
    }

    private fun extractKey(url: String): String =
        URI(url).path.trimEnd('/').substringAfterLast('/')

    private fun parseDateTime(raw: String): LocalDateTime {
        val text = raw.trim()
            .replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
            .replace(". ", ".")

        for (formatter in DATE_FORMATTERS) {
            runCatching { return LocalDate.parse(text, formatter).atStartOfDay() }
        }

        throw IllegalArgumentException("Unsupported date format: $raw")
    }

    internal data class WoowabrosPage(
        val posts: List<TechBlogPost>,
        val maxPage: Int,
    )

    private companion object {
        private const val BASE_URL = "https://techblog.woowahan.com/"
        private const val USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36"

        private val DATE_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("yyyy. MM. dd."),
            DateTimeFormatter.ofPattern("yyyy.MM.dd."),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy. MM. dd"),
            DateTimeFormatter.ofPattern("MMM.dd.yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM. dd. yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM.dd.yyyy.", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM. dd. yyyy.", Locale.ENGLISH),
        )
    }
}
