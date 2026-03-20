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
        ) {
            init {
                if (techBlogName.length + title.length > SUMMARY_TOTAL_MAX_LENGTH) {
                    val titleMaxLength = SUMMARY_TOTAL_MAX_LENGTH - techBlogName.length
                    title = title.truncateWithEllipsis(titleMaxLength)
                }
            }
        }

        data class TechBlogPosts(
            val techBlogIcon: String,
            val techBlogName: String,
            val posts: List<PostDetail>
        ) {
            data class PostDetail(
                var title: String,
                var description: String,
                val thumbnail: String,
                val url: String,
            ) {
                init {
                    title = title.truncateWithEllipsis(DETAIL_TITLE_MAX_LENGTH)
                    description = description.truncateWithEllipsis(DETAIL_DESCRIPTION_MAX_LENGTH)
                }
            }
        }

        companion object {
            private const val SUMMARY_TOTAL_MAX_LENGTH = 46
            private const val DETAIL_TITLE_MAX_LENGTH = 34
            private const val DETAIL_DESCRIPTION_MAX_LENGTH = 254
        }
    }
}

private const val ELLIPSIS = "..."

private fun String.truncateWithEllipsis(maxLength: Int): String {
    if (this.length <= maxLength) return this
    if (maxLength <= ELLIPSIS.length) return ELLIPSIS
    return this.take(maxLength - ELLIPSIS.length) + ELLIPSIS
}
