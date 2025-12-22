package server.application.cache

interface PostViewCountCache {
    suspend fun incr(postId: Long)
}