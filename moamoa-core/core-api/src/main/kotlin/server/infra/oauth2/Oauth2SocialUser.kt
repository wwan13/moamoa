package server.infra.oauth2

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User
import server.feature.member.domain.MemberRole
import server.feature.member.domain.Provider
import java.util.UUID
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

sealed class Oauth2SocialUser : OAuth2User {
    data class Authenticated(
        val memberId: Long,
        val role: MemberRole,
        val isNew: Boolean,
    ) : Oauth2SocialUser()

    data class EmailRequired(
        val provider: Provider,
        val providerKey: String,
    ) : Oauth2SocialUser()

    data class HasError(
        val message: String,
    ) : Oauth2SocialUser()

    @Suppress("UNCHECKED_CAST")
    override fun getAttributes(): Map<String, Any> {
        val baseProps: Set<String> = Oauth2SocialUser::class.memberProperties.map { it.name }.toSet()

        val props: Collection<KProperty1<Any, *>> =
            this::class.memberProperties
                .filter { it.name !in baseProps }
                .map { it as KProperty1<Any, *> }

        return buildMap {
            for (p in props) {
                val v = runCatching { p.get(this) }.getOrNull()
                if (v != null) put(p.name, v)
            }
        }
    }

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return emptySet()
    }

    override fun getName(): String {
        return UUID.randomUUID().toString()
    }
}
