package server.core.support.domain

data class ListEntry<T>(
    val count: Long,
    val list: List<T>,
)
