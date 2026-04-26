package server.core.feature.feedback.application

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import server.core.feature.feedback.domain.FeedbackType

data class FeedbackCreateCommand(
    val type: FeedbackType,

    @field:Size(max = 1000)
    val content: String,

    @field:NotBlank
    @field:Email
    val email: String,
)

data class FeedbackCreateResult(
    val feedbackId: Long
)
