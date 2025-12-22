package server.utill.jsoup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import server.utill.webclient.PagingFinishedException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

fun <T> fetchWithPaging(
    targetCount: Int?,
    buildUrl: (page: Int) -> String,
    startPage: Int = 1,
    concurrency: Int = 10,
    timeoutMs: Int = 10_000,
    parse: (doc: Document) -> List<T>
): Flow<T> = channelFlow {
    val stop = AtomicBoolean(false)
    val sem = Semaphore(concurrency)
    val emitted = AtomicInteger(0)
    val jobs = mutableListOf<Job>()

    fun canContinue(): Boolean {
        val remaining = targetCount?.let { it - emitted.get() }
        return remaining == null || remaining > 0
    }

    var page = startPage

    while (!stop.get() && canContinue()) {
        val currentPage = page++

        val job = launch(Dispatchers.IO) {
            sem.withPermit {
                if (stop.get() || !canContinue()) return@withPermit

                try {
                    val doc = jsoup(buildUrl(currentPage), timeoutMs)
                    val items = parse(doc)
                    if (items.isEmpty()) throw PagingFinishedException()

                    for (item in items) {
                        if (stop.get() || !canContinue()) break
                        send(item)
                        emitted.incrementAndGet()
                    }
                } catch (_: PagingFinishedException) {
                    stop.set(true)
                }
            }
        }

        jobs += job
    }

    jobs.joinAll()
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