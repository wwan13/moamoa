package server.client.techblogs

interface TechBlogClient {
    fun getPosts(size: Int? = null): List<TechBlogPost>
}