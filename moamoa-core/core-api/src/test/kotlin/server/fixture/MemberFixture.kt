package server.fixture

import server.feature.member.command.domain.Member
import server.feature.member.command.domain.MemberRole
import server.feature.member.command.domain.Provider

fun createMember(
    id: Long = 0L,
    role: MemberRole = MemberRole.USER,
    email: String = "user@example.com",
    password: String = "encoded-password",
    provider: Provider = Provider.INTERNAL,
    providerKey: String = ""
): Member = Member(
    id = id,
    role = role,
    email = email,
    password = password,
    provider = provider,
    providerKey = providerKey
)
