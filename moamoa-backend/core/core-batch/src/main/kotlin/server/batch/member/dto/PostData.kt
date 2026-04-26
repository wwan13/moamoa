package server.batch.member.dto

internal data class PostData(
    val postId: Long,
    val postTitle: String,
    val postDescription: String,
    val postThumbnail: String,
    val postUrl: String,

    val techBlogId: Long,
    val techBlogTitle: String,
    val techBlogIcon: String
)