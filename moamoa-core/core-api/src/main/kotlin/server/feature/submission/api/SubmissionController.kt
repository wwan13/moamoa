package server.feature.submission.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.feature.submission.application.SubmissionCreateCommand
import server.feature.submission.application.SubmissionCreateResult
import server.feature.submission.application.SubmissionService
import server.security.Passport
import server.security.RequestPassport

@RestController
@RequestMapping("/api/submission")
class SubmissionController(
    private val submissionService: SubmissionService
) {

    @PostMapping
    suspend fun create(
        @Valid @RequestBody command: SubmissionCreateCommand,
        @RequestPassport passport: Passport
    ): ResponseEntity<SubmissionCreateResult> {
        val response = submissionService.create(command, passport.memberId)

        return ResponseEntity.ok(response)
    }
}