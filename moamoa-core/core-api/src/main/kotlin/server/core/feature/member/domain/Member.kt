package server.core.feature.member.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import server.core.support.domain.BaseEntity

@Entity
@Table(
    name = "member",
    uniqueConstraints = [UniqueConstraint(name = "uk_member_email", columnNames = ["email"])]
)
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    override val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 50, nullable = false)
    val role: MemberRole = MemberRole.USER,

    @Column(name = "email", length = 255, nullable = false)
    val email: String,

    password: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 128, nullable = false)
    val provider: Provider,

    @Column(name = "provider_key", length = 256, nullable = false)
    val providerKey: String,
) : BaseEntity() {
    @Column(name = "pw", length = 255, nullable = false)
    var password: String = password
        private set


    fun created() = MemberCreateEvent(
        memberId = id,
        email = email,
    )

    fun updatePassword(encodedPassword: String) {
        password = encodedPassword
    }

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
