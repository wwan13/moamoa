package server.application

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL

data class SubmissionCreateCommand(
    @field:NotBlank
    @field:Size(max = 255)
    val blogTitle: String,

    @field:NotBlank
    @field:URL
    val blogUrl: String
)

data class SubmissionCreateResult(
    val submissionId: Long
)
