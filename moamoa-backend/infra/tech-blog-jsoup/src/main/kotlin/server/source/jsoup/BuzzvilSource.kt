package server.source.jsoup

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.Flow
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import server.source.common.PagingFinishedException
import server.source.jsoup.util.fetchWithPaging
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import java.net.URI
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
internal class BuzzvilSource : TechBlogSource {

    private val baseUrl = "https://tech.buzzvil.com"
    private val timeoutMs = 10_000

    override suspend fun getPosts(size: Int?): Flow<TechBlogPost> {
        var pageNo = 0
        var firstPageSignature: String? = null

        return fetchWithPaging(
            targetCount = size,
            buildUrl = ::buildListUrl,
            timeoutMs = timeoutMs
        ) { doc ->
            pageNo++

            val posts = parsePosts(doc)
            if (posts.isEmpty()) throw PagingFinishedException()

            val signature = posts.map { it.url.trim() }
                .filter { it.isNotBlank() }
                .joinToString("|")

            if (signature.isBlank()) throw PagingFinishedException()
            if (pageNo == 1) firstPageSignature = signature
            if (pageNo > 1 && firstPageSignature == signature) throw PagingFinishedException()

            posts
        }
    }

    private fun buildListUrl(page: Int): String =
        if (page == 1) "$baseUrl/blog" else "$baseUrl/blog?page=$page"

    internal fun parsePosts(doc: Document): List<TechBlogPost> {
        val postsJson = extractPostsJson(doc)
        if (postsJson.isBlank()) return emptyList()

        return runCatching {
            objectMapper.readTree(postsJson)
                .mapNotNull(::parsePostNode)
                .distinctBy { it.url }
        }.getOrDefault(emptyList())
    }

    private fun parsePostNode(node: JsonNode): TechBlogPost? {
        val slug = node.path("slug").asText().trim().takeIf { it.isNotBlank() } ?: return null
        val url = "$baseUrl/blog/$slug"
        val title = node.path("title").asText().trim().takeIf { it.isNotBlank() } ?: return null
        val publishedAt = parsePublishedAt(node.path("date").asText()) ?: return null
        val category = node.path("category").asText().trim().takeIf { it.isNotBlank() }
        val tags = node.path("tags")
            .mapNotNull { it.asText().trim().takeIf(String::isNotBlank) }
            .let { values -> listOfNotNull(category).plus(values) }
            .distinct()

        return TechBlogPost(
            key = extractKey(url),
            title = title,
            description = node.path("summary").asText().trim(),
            tags = tags,
            thumbnail = resolveUrl(node.path("coverUrl").asText()),
            publishedAt = publishedAt,
            url = url
        )
    }

    private fun extractKey(url: String): String =
        URI(url).path.trimEnd('/').substringAfterLast('/')

    private fun parsePublishedAt(raw: String): LocalDateTime? {
        val text = raw.trim()
        if (text.isBlank()) return null

        return listOfNotNull(
            runCatching { ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime() }.getOrNull(),
            runCatching { OffsetDateTime.parse(text).toLocalDateTime() }.getOrNull(),
            parseJavaScriptDate(text),
        ).firstOrNull(::isPersistableDate)
    }

    private fun parseJavaScriptDate(text: String): LocalDateTime? {
        val match = javaScriptDateRegex.matchEntire(text) ?: return null
        val month = monthNumbers[match.groupValues[1]] ?: return null
        val day = match.groupValues[2].toInt()
        val year = match.groupValues[3].toInt()
        val hour = match.groupValues[4].toInt()
        val minute = match.groupValues[5].toInt()
        val second = match.groupValues[6].toInt()
        val offset = match.groupValues[7].let { value ->
            ZoneOffset.of("${value.substring(0, 3)}:${value.substring(3, 5)}")
        }

        return OffsetDateTime.of(year, month, day, hour, minute, second, 0, offset).toLocalDateTime()
    }

    private fun isPersistableDate(dateTime: LocalDateTime): Boolean {
        val year = dateTime.year
        return year in 1900..2100
    }

    private fun resolveUrl(raw: String): String {
        val value = raw.trim()
        if (value.isBlank()) return ""
        return URI(baseUrl).resolve(value).toString()
    }

    private fun extractPostsJson(doc: Document): String {
        val decodedPayload = doc.select("script")
            .joinToString("\n") { script ->
                decodeNextFlightScript(script.data().ifBlank { script.html() })
            }

        val postsStart = listOf("\"items\":[", "\"posts\":[")
            .map { decodedPayload.indexOf(it) to it }
            .filter { (index, _) -> index >= 0 }
            .minByOrNull { (index, _) -> index }
            ?: return ""

        val keyIndex = postsStart.first
        val key = postsStart.second

        val arrayStart = keyIndex + key.substringBefore('[').length
        return decodedPayload.extractJsonArray(arrayStart)
    }

    private fun decodeNextFlightScript(script: String): String {
        val encoded = nextFlightStringRegex.find(script)?.groupValues?.getOrNull(1) ?: return script
        return runCatching {
            objectMapper.readValue("\"$encoded\"", String::class.java)
        }.getOrDefault(script)
    }

    private fun String.extractJsonArray(startIndex: Int): String {
        if (startIndex !in indices || this[startIndex] != '[') return ""

        var depth = 0
        var inString = false
        var escaped = false

        for (index in startIndex until length) {
            val char = this[index]

            if (escaped) {
                escaped = false
                continue
            }

            if (char == '\\') {
                escaped = true
                continue
            }

            if (char == '"') {
                inString = !inString
                continue
            }

            if (inString) continue

            if (char == '[') depth++
            if (char == ']') {
                depth--
                if (depth == 0) return substring(startIndex, index + 1)
            }
        }

        return ""
    }

    private val javaScriptDateRegex = Regex(
        """[A-Za-z]{3}\s+([A-Za-z]{3})\s+(\d{1,2})\s+(\d{4})\s+(\d{2}):(\d{2}):(\d{2})\s+GMT([+-]\d{4})\s+\(.+\)"""
    )
    private val monthNumbers = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
        .let { formatter ->
            (1..12).associateBy { month ->
                java.time.Month.of(month).getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)
            }
        }

    private val nextFlightStringRegex = Regex("""self\.__next_f\.push\(\[1,"(.*)"\]\)""", RegexOption.DOT_MATCHES_ALL)
    private val objectMapper = ObjectMapper()
}
