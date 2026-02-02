package server.techblogs.rapportlabs

import kotlinx.coroutines.flow.Flow
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import server.utill.PagingFinishedException
import server.utill.fetchWithPaging
import server.utill.normalizeTagTitle
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
internal class RapportlabsSource : TechBlogSource {

    private val blogKey = "rapportlabs"
    private val baseUrl = "https://blog.rapportlabs.kr"
    private val timeoutMs = 10_000

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        val seenKeys = HashSet<String>(2048)

        return fetchWithPaging(size, ::buildListUrl, timeoutMs = timeoutMs) { doc ->
            val linkEls = doc.select("main a[href]")
            if (linkEls.isEmpty()) throw PagingFinishedException()

            val parsed = linkEls.mapNotNull { linkEl ->
                val url = linkEl.absUrl("href").trim()
                if (url.isBlank() || !url.startsWith(baseUrl)) return@mapNotNull null

                val key = extractKeyFromUrl(url) ?: return@mapNotNull null

                val title = requireField(url, "title", linkEl.selectFirst("h1, h2, h3")?.text())
                val description = extractDescription(linkEl)

                val dateText = extractDateText(linkEl)
                val publishedAt = dateText?.let(::parsePublishedAt) ?: LocalDateTime.MIN

                val tags = extractTags(linkEl, dateText)
                val thumbnail = extractThumbnail(linkEl, url)

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

            val unique = parsed.filter { seenKeys.add(it.key) }
            if (unique.isEmpty()) throw PagingFinishedException()

            unique
        }
    }

    private fun buildListUrl(page: Int): String =
        if (page == 1) baseUrl else "$baseUrl?page=$page"

    private fun extractKeyFromUrl(url: String): String? {
        val path = runCatching { URI(url).path }.getOrNull()?.trim('/')?.trim().orEmpty()
        if (path.isBlank()) return null
        if (path.startsWith("category/") || path.startsWith("tag/")) return null
        return path
    }

    private fun extractDescription(root: Element): String {
        val candidate = root.select("div[class*=line-clamp], p")
            .firstOrNull { it.selectFirst("span") == null }
            ?.text()
            ?.trim()

        return candidate.orEmpty()
    }

    private fun extractDateText(root: Element): String? {
        val timeAttr = root.selectFirst("time[datetime]")?.attr("datetime")?.trim()
        if (!timeAttr.isNullOrBlank()) return timeAttr

        val candidates = root.select("span, div")
            .asSequence()
            .map { it.text().trim() }
            .filter { it.isNotBlank() }

        for (text in candidates) {
            val match = DATE_REGEX.find(text) ?: continue
            return match.value
        }

        return null
    }

    private fun extractTags(root: Element, dateText: String?): List<String> {
        val tagCandidates = root.select("span[class*=rounded-full]")
            .map { it.text().trim() }
            .ifEmpty { root.select("span").map { it.text().trim() } }

        return tagCandidates
            .asSequence()
            .filter { it.isNotBlank() }
            .filterNot { it == dateText }
            .filterNot { it.matches(DATE_REGEX) }
            .filterNot { it.length <= 2 && it.all { ch -> ch.isLetter() && ch.isLowerCase() } }
            .map { it.normalizeTagTitle() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    private fun extractThumbnail(root: Element, url: String): String {
        val img = root.selectFirst("img[src], img[srcset]")

        val src = img?.attr("src")?.trim().orEmpty()
        if (src.isNotBlank()) return requireField(url, "thumbnail", resolveImageUrl(src))

        val srcset = img?.attr("srcset")?.trim().orEmpty()
        val firstSrc = srcset.split(',')
            .firstOrNull()
            ?.trim()
            ?.substringBefore(' ')
            ?.trim()
            .orEmpty()

        return requireField(url, "thumbnail", resolveImageUrl(firstSrc))
    }

    private fun resolveImageUrl(raw: String): String = when {
        raw.startsWith("http") -> raw
        raw.startsWith("//") -> "https:$raw"
        raw.startsWith("/") -> baseUrl + raw
        else -> raw
    }

    private fun parsePublishedAt(raw: String): LocalDateTime {
        runCatching { return OffsetDateTime.parse(raw).toLocalDateTime() }
        DATE_FORMATTERS.forEach { formatter ->
            runCatching { return LocalDate.parse(raw.trim(), formatter).atStartOfDay() }
        }
        return LocalDateTime.MIN
    }

    private fun requireField(url: String, field: String, value: String?): String {
        val resolved = value?.trim().orEmpty()
        if (resolved.isBlank()) {
            throw IllegalStateException("blogKey=$blogKey url=$url field=$field")
        }
        return resolved
    }

    companion object {
        private val DATE_REGEX =
            Regex("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{1,2},\\s+\\d{4}")

        private val DATE_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)
        )
    }
}
