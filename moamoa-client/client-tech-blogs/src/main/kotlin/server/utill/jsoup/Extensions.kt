package server.utill.jsoup

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

fun jsoup(url: String, timeoutMs: Int = 10_000): Document {
    val response = Jsoup.connect(url)
        .timeout(timeoutMs)
        .userAgent(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36"
        )
        .referrer("https://www.google.com")
        .followRedirects(true)
        .ignoreHttpErrors(true) // ⭐ 중요
        .execute()

    val status = response.statusCode()
    if (status == 403) {
        throw HttpStatusException(403, "Forbidden: $url")
    }
    if (status >= 400) {
        throw HttpStatusException(status, "HTTP $status: $url")
    }

    return response.parse()
}