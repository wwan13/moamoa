package server.feature.member.command.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.domain.BaseEntity

@Table("member")
data class Member(
    @Id
    @Column("id")
    override val id: Long = 0,

    @Column("role")
    val role: MemberRole = MemberRole.USER,

    @Column("email")
    val email: String,

    @Column("pw")
    val password: String,

    @Column("provider")
    val provider: Provider,

    @Column("provider_key")
    val providerKey: String,
) : BaseEntity() {

    fun created() = MemberCreateEvent(
        memberId = id,
        email = email,
    )

    companion object {
        fun fromInternal(
            email: String,
            password: String
        ) = Member(
            email = email,
            password = password,
            provider = Provider.INTERNAL,
            providerKey = ""
        )

        fun fromSocial(
            email: String,
            provider: Provider,
            providerKey: String
        ): Member {
            if (provider == Provider.INTERNAL) {
                throw IllegalStateException("소셜 로그인으로 회원가입한 유저입니다.")
            }

            return Member(
                email = email,
                provider = provider,
                providerKey = providerKey,
                password = "",
            )
        }
    }
}
