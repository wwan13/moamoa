package support.domain

data class DomainModified<T>(
    val entity: T,
    val event: Any
)
