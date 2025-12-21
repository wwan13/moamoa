package server.utill.jsoup

class HttpStatusException(
    val statusCode: Int,
    message: String
) : RuntimeException(message)