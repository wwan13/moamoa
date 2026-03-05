package server.lock

class LockInfraException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
