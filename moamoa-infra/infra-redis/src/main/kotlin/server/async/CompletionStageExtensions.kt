package server.async

import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CompletionStage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> CompletionStage<T>.await(): T = suspendCancellableCoroutine { continuation ->
    val future = toCompletableFuture()
    future.whenComplete { value, throwable ->
        if (throwable != null) {
            continuation.resumeWithException(throwable)
        } else {
            continuation.resume(value)
        }
    }

    continuation.invokeOnCancellation {
        future.cancel(true)
    }
}
