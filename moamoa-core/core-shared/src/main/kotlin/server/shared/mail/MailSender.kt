package server.shared.mail

interface MailSender {
    suspend fun send(mailContent: MailContent)
}
