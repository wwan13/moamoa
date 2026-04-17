package server.core.feature.feedback.domain

import server.messaging.Event

data class FeedbackCreateEvent(
    val feedbackId: Long,
    val feedbackType: FeedbackType,
    val content: String,
    val email: String,
) : Event
