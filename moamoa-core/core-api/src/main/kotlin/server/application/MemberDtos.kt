package server.application

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import server.domain.member.Member

data class EmailExistsCommand(
    @field:NotBlank
    @field:Email
    val email: String
)

data class CreateMemberCommand(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    @field:Size(min = 6, max = 24)
    @field:Pattern(regexp = "^(?=.*[^A-Za-z0-9])[A-Za-z0-9[^A-Za-z0-9]]{8,32}$")
    val password: String,

    @field:NotBlank
    @field:Size(min = 6, max = 24)
    @field:Pattern(regexp = "^(?=.*[^A-Za-z0-9])[A-Za-z0-9[^A-Za-z0-9]]{8,32}$")
    val passwordConfirm: String
)

data class MemberData(
    val id: Long,
    val email: String
) {
    constructor(member: Member) : this(
        id = member.id,
        email = member.email
    )
}

data class EmailExistsResult(
    val exists: Boolean
)