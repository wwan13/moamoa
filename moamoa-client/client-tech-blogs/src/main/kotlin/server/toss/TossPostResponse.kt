package server.toss

data class TossPostResponse(
    val resultType: String,
    val success: Success?
) {
    data class Success(
        val page: Int,
        val pageSize: Int,
        val count: Int,
        val next: String?,
        val previous: String?,
        val results: List<Post>
    ) {
        data class Post(
            val id: Long,
            val key: String,
            val title: String,
            val subtitle: String,
            val category: String,
            val publishedTime: String,
            val thumbnailConfig: ThumbnailConfig,
        ) {
            data class ThumbnailConfig(
                val imageUrl: String
            )
        }
    }
}
