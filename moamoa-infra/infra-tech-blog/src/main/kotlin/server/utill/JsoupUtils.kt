package server.utill

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import server.utill.PagingFinishedException

fun <T> fetchWithPaging(
    targetCount: Int?,
    buildUrl: (page: Int) -> String,
    startPage: Int = 1,
    timeoutMs: Int = 10_000,
    parse: (doc: Document) -> List<T>
): Flow<T> = flow {
    var page = startPage
    var emitted = 0

    fun canContinue() = targetCount == null || emitted < targetCount

    while (canContinue()) {
        val items=  try {
            val doc = withContext(Dispatchers.IO) {
                jsoup(buildUrl(page), timeoutMs)
            }
            parse(doc)
        } catch (_: PagingFinishedException) {
            break
        }

        if (items.isEmpty()) break

        for (item in items) {
            emit(item)
            emitted++
            if (!canContinue()) break
        }

        page++
    }
}

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
        .ignoreHttpErrors(true)
        .execute()

    val status = response.statusCode()

    if (status == 403 || status == 404) throw PagingFinishedException()

    if (status >= 400) throw PagingFinishedException()

    return response.parse()
}