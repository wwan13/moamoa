package server.client.techblogs

import kotlinx.coroutines.flow.Flow

interface TechBlogClient {
    suspend fun getPosts(size: Int? = null): Flow<TechBlogPost>
}