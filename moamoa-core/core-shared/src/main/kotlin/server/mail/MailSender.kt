package server.mail

interface MailSender {
    suspend fun send(mailContent: MailContent)
}
