package server.member.dto

data class AlarmContent(
    val memberId: Long,
    val email: String,
    val techBlog: List<TechBlog>
) {
    data class TechBlog(
        val techBlogId: Long,
        val techBlogTitle: String,
        val techBlogIcon: String,

        val posts: List<Post>
    ) {
        data class Post(
            val postId: Long,
            val postTitle: String,
            val postDescription: String,
            val postThumbnail: String,
            val postUrl: String
        )
    }
}
