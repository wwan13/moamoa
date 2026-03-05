package server.cache

class CacheInfraException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
