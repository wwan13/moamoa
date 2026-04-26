package server.admin.feature.notice.application

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

internal data class AdminNoticeCreateCommand(
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

internal data class AdminUpdateNoticePublishedCommand(
    val published: Boolean,
)
