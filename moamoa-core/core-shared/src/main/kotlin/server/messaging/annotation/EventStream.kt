package server.messaging.annotation

enum class EventStream(
    val channel: EventChannel,
    val consumerGroup: String,
    val ackOnFailure: Boolean,
    val processSequentially: Boolean,
    val batchSize: Int,
) {
    DEFAULT(
        EventChannel.DEFAULT,
        "default-group",
        true,
        false,
        10
    ),

    COUNT_PROCESSING(
        EventChannel.DEFAULT,
        "count-processing-group",
        true,
        false,
        10
    ),
    POST_CACHE_HANDLING(
        EventChannel.DEFAULT,
        "post-cache-handling-group",
        true,
        false,
        10
    ),
    TECH_BLOG_CACHE_HANDLING(
        EventChannel.DEFAULT,
        "tech-blog-cache-handling-group",
        true,
        false,
        10
    ),
    MONITORING(
        EventChannel.DEFAULT,
        "monitoring-group",
        false,
        false,
        10
    ),
    DEFAULT_DLQ(
        EventChannel.DEFAULT_DLQ,
        "dlq-group",
        true,
        false,
        10
    ),
}
