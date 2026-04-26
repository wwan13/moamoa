package server.core.feature.feedback.api

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.core.feature.feedback.application.FeedbackCreateCommand
import server.core.feature.feedback.application.FeedbackCreateResult
import server.core.feature.feedback.application.FeedbackService
import server.core.global.web.ApiResponse

@RestController
@RequestMapping("/api/feedback")
class FeedbackController(
    private val feedbackService: FeedbackService
) {

    @PostMapping
    fun create(
        @RequestBody command: FeedbackCreateCommand
    ): ApiResponse<FeedbackCreateResult> {
        val response = feedbackService.create(command)

        return ApiResponse.of(response)
    }
}
