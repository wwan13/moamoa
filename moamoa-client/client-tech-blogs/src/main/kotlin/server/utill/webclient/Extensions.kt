package server.utill.webclient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> fetchWithPaging(
    pageSize: Int,
    targetCount: Int? = null,
    startPage: Int = 1,
    concurrency: Int = 10,
    fetch: suspend (size: Int, page: Int) -> List<T>
): Flow<T> = channelFlow {
    val stop = AtomicBoolean(false)
    val semaphore = Semaphore(concurrency)
    val jobs = mutableListOf<Job>()

    var page = startPage
    var planned = 0

    fun nextSize(): Int {
        val remaining = targetCount?.let { it - planned }
        if (remaining != null && remaining <= 0) return 0
        return remaining?.let { minOf(pageSize, it) } ?: pageSize
    }

    while (!stop.get()) {
        val size = nextSize()
        if (size <= 0) break
        if (stop.get()) break

        val currentPage = page
        page++
        planned += size

        val job = launch(Dispatchers.IO) {
            semaphore.withPermit {
                if (stop.get()) return@withPermit

                try {
                    fetch(size, currentPage).forEach { send(it) }
                } catch (_: PagingFinishedException) {
                    stop.set(true)
                }
            }
        }
        jobs += job
    }

    jobs.joinAll()
}

fun WebClient.ResponseSpec.handlePagingFinished(): WebClient.ResponseSpec = this
    .onStatus(
        { it == HttpStatus.BAD_REQUEST || it == HttpStatus.NOT_FOUND }
    ) {
        Mono.error(PagingFinishedException())
    }

fun Collection<*>.validateIsPagingFinished() {
    if (isNullOrEmpty()) {
        throw PagingFinishedException()
    }
}
