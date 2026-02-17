package server.shared.messaging

data class SubscriptionDefinition(
    val channel: MessageChannel,
    val consumerGroup: String,
    val ackOnFailure: Boolean = false,
    val processSequentially: Boolean = true,
    val batchSize: Int = 1,
)
