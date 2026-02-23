package server.shared.lock

class LockInfraException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
