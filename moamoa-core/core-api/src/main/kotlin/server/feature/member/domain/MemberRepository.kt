package server.feature.member.domain

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface MemberRepository : CoroutineCrudRepository<Member, Long> {
    suspend fun findByEmail(email: String): Member?

    suspend fun findByProviderAndProviderKey(provider: Provider, providerKey: String): Member?

    suspend fun existsByEmail(email: String): Boolean
}