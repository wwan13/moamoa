package server.core.feature.submission.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.core.feature.submission.application.SubmissionCreateCommand
import server.core.feature.submission.application.SubmissionCreateResult
import server.core.feature.submission.application.SubmissionService
import server.core.global.security.Passport
import server.core.global.security.RequestPassport

@RestController
@RequestMapping("/api/submission")
class SubmissionController(
    private val submissionService: SubmissionService
) {

    @PostMapping
    fun create(
        @Valid @RequestBody command: SubmissionCreateCommand,
        @RequestPassport passport: Passport
    ): ResponseEntity<SubmissionCreateResult> {
        val response = submissionService.create(command, passport.memberId)

        return ResponseEntity.ok(response)
    }
}