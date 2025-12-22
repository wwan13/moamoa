package server.application

import server.domain.member.Member

data class CreateMemberCommand(
    val email: String,
    val password: String,
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