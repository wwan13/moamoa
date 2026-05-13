package server.techblog.http

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource

internal class PythonCrawlerTechBlogSource(
    private val client: TechBlogCrawlerClient,
) : TechBlogSource {

    override suspend fun getPosts(key: String, size: Int?): Flow<TechBlogPost> {
        return client.crawl(key = key, size = size).posts.asFlow()
    }

    override fun exists(key: String): Boolean = runBlocking {
        runCatching { client.crawl(key = key, size = 1) }.isSuccess
    }
}
