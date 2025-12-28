package server.application

data class ListMeta(
    val page: Long,
    val size: Long,
    val totalCount: Long,
    val totalPages: Long
)