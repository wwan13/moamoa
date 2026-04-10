package server.techblog

import kotlinx.coroutines.flow.Flow

interface TechBlogSource {
    suspend fun getPosts(size: Int? = null): Flow<TechBlogPost>
}