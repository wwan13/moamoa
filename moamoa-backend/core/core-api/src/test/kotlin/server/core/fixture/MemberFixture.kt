package server.core.fixture

import server.core.feature.member.domain.Member
import server.core.feature.member.domain.MemberRole
import server.core.feature.member.domain.MemberProvider

fun createMember(
    id: Long = 0L,
    role: MemberRole = MemberRole.USER,
    email: String = "user@example.com",
    password: String = "encoded-password",
    provider: MemberProvider = MemberProvider.INTERNAL,
    providerKey: String = ""
): Member = Member(
    id = id,
    role = role,
    email = email,
    password = password,
    provider = provider,
    providerKey = providerKey
)
