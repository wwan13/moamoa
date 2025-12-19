package server.paging

import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

fun <T> fetchWithPaging(
    pageSize: Int,
    targetCount: Int? = null,
    startPage: Int = 1,
    fetch: (size: Int, page: Int) -> List<T>
): List<T> {
    val result = mutableListOf<T>()
    var page = startPage

    while (true) {
        val remaining = targetCount?.let { it - result.size }
        if (remaining != null && remaining <= 0) break

        val size = remaining?.let { minOf(pageSize, it) } ?: pageSize

        val fetched = try {
            fetch(size, page)
        } catch (e: PagingFinishedException) {
            break
        }

        result += fetched
        page++
    }

    return result
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
