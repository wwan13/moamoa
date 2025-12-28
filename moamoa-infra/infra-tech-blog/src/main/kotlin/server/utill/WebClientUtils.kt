package server.utill

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import server.utill.PagingFinishedException

fun <T> fetchWithPaging(
    pageSize: Int,
    targetCount: Int? = null,
    startPage: Int = 1,
    fetch: suspend (size: Int, page: Int) -> List<T>
): Flow<T> = flow {
    var page = startPage
    var sent = 0

    fun canContinue() = targetCount == null || sent < targetCount

    while (canContinue()) {
        val remaining = targetCount?.let { it - sent }
        val size = remaining?.let { minOf(pageSize, it) } ?: pageSize

        val items = try {
            fetch(size, page)
        } catch (_: PagingFinishedException) {
            break
        }

        if (items.isEmpty()) break

        for (item in items) {
            emit(item)
            sent++
            if (!canContinue()) break
        }

        page++
    }
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
