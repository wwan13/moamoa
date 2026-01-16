package server.feature.member.application

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import server.feature.member.domain.Member
import server.feature.member.domain.MemberRole
import server.feature.member.domain.Provider

data class EmailExistsCommand(
    @field:NotBlank
    @field:Email
    val email: String
)

data class CreateInternalMemberCommand(
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

data class CreateSocialMemberCommand(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    val provider: Provider,

    @field:NotBlank
    val providerKey: String
)

data class MemberData(
    val id: Long,
    val email: String,
    val role: MemberRole,
) {
    constructor(member: Member) : this(
        id = member.id,
        email = member.email,
        role = member.role,
    )
}

data class EmailExistsResult(
    val exists: Boolean
)