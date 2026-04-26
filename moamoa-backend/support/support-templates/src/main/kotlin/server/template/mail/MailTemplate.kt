package server.template.mail

sealed interface MailTemplate {
    val path: String

    data class EmailVerification(
        val homeUrl: String,
        val verificationCode: String,
    ) : MailTemplate {
        override val path: String = "email-verification"
    }

    data class NewPosts(
        val date: String,
        val count: Long,
        val postSummaries: List<PostSummary>,
        val techBlogPosts: List<TechBlogPosts>
    ) : MailTemplate {
        override val path: String = "new-posts"

        data class PostSummary(
            val techBlogId: Long,
            val techBlogIcon: String,
            val techBlogName: String,

            var title: String,
            val url: String,
        )

        data class TechBlogPosts(
            val techBlogId: Long,
            val techBlogIcon: String,
            val techBlogName: String,
            val posts: List<PostDetail>
        ) {
            data class PostDetail(
                var title: String,
                var description: String,
                val thumbnail: String,
                val url: String
            )
        }
    }

    data class ApplyTemporaryPassword(
        val password: String
    ) : MailTemplate {
        override val path: String = "apply-temporary-password"
    }
}
