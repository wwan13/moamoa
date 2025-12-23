package server

sealed class MailContent{

    abstract val to: String
    abstract val subject: String

    data class Text(
        override val to: String,
        override val subject: String,
        val text: String
    ) : MailContent()

    data class Html(
        override val to: String,
        override val subject: String,
        val text: String
    ) : MailContent()

    data class Template(
        override val to: String,
        override val subject: String,
        val path: String,
        val args: Map<String, Any>
    ) : MailContent()
}