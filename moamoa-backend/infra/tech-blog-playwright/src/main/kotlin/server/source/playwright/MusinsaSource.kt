package server.source.playwright

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
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
internal class MusinsaSource(
    private val playwrightProperties: PlaywrightProperties = PlaywrightProperties(),
    private val clock: Clock = Clock.systemDefaultZone(),
) : TechBlogSource {

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> = flow {
        val posts = withContext(Dispatchers.IO) {
            scrapePosts(size)
        }

        posts.forEach { emit(it) }
    }

    internal fun parsePostSnapshot(snapshot: MusinsaPostSnapshot): TechBlogPost {
        val url = snapshot.url.substringBefore('?').trim()
            .ifBlank { throw IllegalArgumentException("post url is blank") }
        val title = snapshot.title.trim()
            .ifBlank { throw IllegalArgumentException("post title is blank") }

        return TechBlogPost(
            key = extractKey(url),
            title = title,
            description = snapshot.description.trim(),
            tags = snapshot.tags.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
            thumbnail = snapshot.thumbnail.trim(),
            publishedAt = parseDateTime(snapshot.publishedAt)
                ?: throw IllegalArgumentException("post publishedAt is invalid. url=$url value=${snapshot.publishedAt}"),
            url = url,
        )
    }

    private fun scrapePosts(size: Int?): List<TechBlogPost> {
        Playwright.create().use { playwright ->
            openBrowser(playwright).use { browser ->
                browser.newContext(
                    Browser.NewContextOptions()
                        .setUserAgent(USER_AGENT)
                        .setLocale("ko-KR")
                ).use { context ->
                    PlaywrightStealth.applyTo(context)
                    val page = context.newPage()
                    page.navigate(ALL_POSTS_URL, Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED))
                    page.waitForLoadState()
                    println("[MusinsaSource] list loaded url=${page.url()} title=${page.title()}")
                    println("[MusinsaSource] list text=${pageBodyPreview(page)}")

                    val urls = fetchPostUrls(page, size)
                    println("[MusinsaSource] collected urls size=${urls.size} urls=${urls.take(DEBUG_PRINT_LIMIT)}")
                    return urls.mapNotNull { url ->
                        runCatching { fetchPost(context, url) }
                            .onFailure { println("[MusinsaSource] failed url=$url error=${it.message}") }
                            .getOrNull()
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
            PlaywrightStealth.launchOptions()
        )
    }

    private fun fetchPostUrls(page: Page, size: Int?): List<String> {
        page.waitForTimeout(2_000.0)

        val urls = linkedSetOf<String>()
        var stableScrollCount = 0
        var previousHeight = 0

        while ((size == null || urls.size < size) && stableScrollCount < MAX_STABLE_SCROLL_COUNT) {
            val extractedUrls = extractPostUrls(page)
            urls += extractedUrls
            println(
                "[MusinsaSource] scroll urls extracted=${extractedUrls.size} total=${urls.size} " +
                        "stableScrollCount=$stableScrollCount sample=${extractedUrls.take(DEBUG_PRINT_LIMIT)}"
            )

            val height = page.evaluate("() => document.body.scrollHeight") as Number
            page.evaluate("() => window.scrollTo(0, document.body.scrollHeight)")
            page.waitForTimeout(SCROLL_WAIT_MILLIS)

            val nextHeight = (page.evaluate("() => document.body.scrollHeight") as Number).toInt()
            println("[MusinsaSource] scroll height before=${height.toInt()} after=$nextHeight")
            stableScrollCount = if (nextHeight <= previousHeight || nextHeight == height.toInt()) {
                stableScrollCount + 1
            } else {
                0
            }
            previousHeight = nextHeight
        }

        return if (size == null) urls.toList() else urls.take(size)
    }

    private fun extractPostUrls(page: Page): List<String> {
        val urls = page.evaluate(
            """
            () => Array.from(document.querySelectorAll('a[href]'))
              .map((anchor) => anchor.href)
              .filter((href) => {
                const url = new URL(href);
                return url.hostname === 'techblog.musinsa.com' ||
                  (url.hostname === 'medium.com' && url.pathname.startsWith('/musinsa-tech/'));
              })
              .filter((href) => /\/[^/?#]+-[0-9a-f]{12}(?:[?#].*)?$/i.test(new URL(href).pathname + new URL(href).search))
              .map((href) => href.split('?')[0])
            """.trimIndent()
        ) as List<*>

        return urls.mapNotNull { it as? String }
    }

    private fun pageBodyPreview(page: Page): String {
        val text = page.evaluate(
            """
            () => document.body?.innerText
              ?.replace(/\s+/g, ' ')
              ?.slice(0, 1000) || ''
            """.trimIndent()
        ) as String
        return text
    }

    private fun fetchPost(context: BrowserContext, url: String): TechBlogPost {
        val page = context.newPage()
        return try {
            page.navigate(url, Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED))
            page.waitForLoadState()
            page.waitForTimeout(1_000.0)
            println("[MusinsaSource] post loaded requestedUrl=$url actualUrl=${page.url()} title=${page.title()}")

            val snapshot = page.evaluate(
                """
                () => {
                  const meta = (name) =>
                    document.querySelector(`meta[property="${'$'}{name}"]`)?.getAttribute('content') ||
                    document.querySelector(`meta[name="${'$'}{name}"]`)?.getAttribute('content') ||
                    '';
                  const text = (selector) => document.querySelector(selector)?.textContent?.trim() || '';
                  const tags = Array.from(document.querySelectorAll('a[href*="/tagged/"], a[href*="/tag/"]'))
                    .map((anchor) => anchor.textContent?.trim() || '')
                    .filter(Boolean);

                  return {
                    url: location.href,
                    title: meta('og:title') || text('h1') || document.title,
                    description: meta('og:description') || text('article p') || text('p'),
                    thumbnail: meta('og:image') || document.querySelector('article img, img')?.src || '',
                    publishedAt: meta('article:published_time') ||
                      meta('datePublished') ||
                      document.querySelector('time[datetime]')?.getAttribute('datetime') ||
                      Array.from(document.querySelectorAll('a, span, div, p'))
                        .map((element) => element.textContent?.trim().replace(/\s+/g, ' ') || '')
                        .find((text) => /(?:^|·)\s*Added\s+(?:\d+\s*[hdwmy]\s+ago|[A-Z][a-z]{2}\.?\s+\d{1,2})(?:\s*·|${'$'})/.test(text)) ||
                      '',
                    tags
                  };
                }
                """.trimIndent()
            ) as Map<*, *>
            println("[MusinsaSource] post snapshot url=$url snapshot=$snapshot")

            parsePostSnapshot(
                MusinsaPostSnapshot(
                    url = snapshot["url"] as? String ?: url,
                    title = snapshot["title"] as? String ?: "",
                    description = snapshot["description"] as? String ?: "",
                    thumbnail = snapshot["thumbnail"] as? String ?: "",
                    publishedAt = snapshot["publishedAt"] as? String ?: "",
                    tags = (snapshot["tags"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
                )
            )
        } finally {
            page.close()
        }
    }

    private fun extractKey(url: String): String =
        URI(url).path.trimEnd('/').substringAfterLast('-')
            .ifBlank { URI(url).path.trimEnd('/').substringAfterLast('/') }

    private fun parseDateTime(raw: String): LocalDateTime? {
        val text = raw.trim()
            .replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
            .replace(Regex("(^|.*?\\b)Added\\s+"), "")
            .replace(Regex("\\s*·.*$"), "")
            .removeSuffix(".")

        if (text.isBlank()) return null

        parseRelativeDateTime(text)?.let { return it.takeIfReasonable() }

        runCatching { return OffsetDateTime.parse(text).toLocalDateTime().takeIfReasonable() }
        runCatching { return ZonedDateTime.parse(text).toLocalDateTime().takeIfReasonable() }

        for (formatter in DATE_TIME_FORMATTERS) {
            runCatching {
                return LocalDateTime.parse(text, formatter).takeIfReasonable()
            }
        }

        for (formatter in DATE_FORMATTERS) {
            runCatching {
                return LocalDate.parse(text, formatter).atStartOfDay().takeIfReasonable()
            }
        }

        for (formatter in DATE_FORMATTERS_WITHOUT_YEAR) {
            runCatching {
                return inferYear(LocalDate.parse("$text ${LocalDate.now(clock).year}", formatter))
                    .atStartOfDay()
                    .takeIfReasonable()
            }
        }

        return null
    }

    private fun parseRelativeDateTime(text: String): LocalDateTime? {
        val match = RELATIVE_DATE_REGEX.matchEntire(text.lowercase(Locale.ENGLISH)) ?: return null
        val amount = match.groupValues[1].toLong()
        val unit = match.groupValues[2]
        val now = LocalDateTime.now(clock)

        return when (unit) {
            "h" -> now.minusHours(amount)
            "d" -> now.minusDays(amount)
            "w" -> now.minusWeeks(amount)
            "m" -> now.minusMonths(amount)
            "y" -> now.minusYears(amount)
            else -> null
        }
    }

    private fun inferYear(date: LocalDate): LocalDate {
        val today = LocalDate.now(clock)
        return if (date.isAfter(today.plusDays(1))) date.minusYears(1) else date
    }

    private fun LocalDateTime.takeIfReasonable(): LocalDateTime? =
        takeIf { it.year in MIN_PUBLISHED_YEAR..MAX_PUBLISHED_YEAR }

    internal data class MusinsaPostSnapshot(
        val url: String,
        val title: String,
        val description: String,
        val thumbnail: String,
        val publishedAt: String,
        val tags: List<String>,
    )

    private companion object {
        private const val ALL_POSTS_URL = "https://techblog.musinsa.com/all"
        private const val MAX_STABLE_SCROLL_COUNT = 3
        private const val SCROLL_WAIT_MILLIS = 1_000.0
        private const val DEBUG_PRINT_LIMIT = 5
        private const val MIN_PUBLISHED_YEAR = 2000
        private const val MAX_PUBLISHED_YEAR = 2100
        private const val USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36"

        private val DATE_TIME_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH),
        )
        private val DATE_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM. d, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy. M. d", Locale.KOREAN),
            DateTimeFormatter.ofPattern("yyyy. MM. dd", Locale.KOREAN),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        )
        private val DATE_FORMATTERS_WITHOUT_YEAR = listOf(
            DateTimeFormatter.ofPattern("MMM d yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM. d yyyy", Locale.ENGLISH),
        )
        private val RELATIVE_DATE_REGEX = Regex("(\\d+)\\s*([hdwmy])\\s+ago")
    }
}
