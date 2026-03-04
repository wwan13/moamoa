package server.async

import java.util.concurrent.CompletionStage

fun <T> CompletionStage<T>.await(): T = toCompletableFuture().get()
