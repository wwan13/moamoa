package server.feature.member.command.domain

import org.springframework.data.jpa.repository.JpaRepository

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: String): Member?

    fun findByProviderAndProviderKey(provider: Provider, providerKey: String): Member?

    fun existsByEmail(email: String): Boolean
}