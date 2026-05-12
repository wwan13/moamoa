package server.techblog

import kotlinx.coroutines.flow.Flow

interface TechBlogSource {
    suspend fun getPosts(key: String, size: Int? = null): Flow<TechBlogPost>

    fun exists(key: String): Boolean
}
