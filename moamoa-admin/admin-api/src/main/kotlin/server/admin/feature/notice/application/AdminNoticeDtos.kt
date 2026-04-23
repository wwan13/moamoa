package server.admin.feature.notice.application

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AdminNoticeCreateCommand(
    @field:NotBlank
    @field:Size(max = 2048)
    val title: String,

    @field:NotBlank
    @field:Size(max = 256)
    val chip: String,

    @field:NotBlank
    val content: String,

    val published: Boolean,
)

data class AdminUpdateNoticePublishedCommand(
    val published: Boolean,
)
