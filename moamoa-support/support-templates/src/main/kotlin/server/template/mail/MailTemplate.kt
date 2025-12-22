package server.template.mail

sealed interface MailTemplate {
    val path: String

    data class EmailVerification(
        val homeUrl: String,
        val verificationCode: String,
    ) : MailTemplate {
        override val path: String = "email-verification"
    }

    data class Welcome(
        val email: String,
    ) : MailTemplate {
        override val path: String = "welcome"
    }
}
