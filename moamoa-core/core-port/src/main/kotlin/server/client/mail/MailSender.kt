package server.client.mail

interface MailSender {
    suspend fun send(mailContent: MailContent)
}