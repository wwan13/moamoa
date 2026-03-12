package server.admin.feature.member.domain

import jakarta.persistence.Entity
import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import server.admin.support.domain.AdminBaseEntity

@Entity
@Table(name = "member")
internal class AdminMember(
    @Id
    @Column(name = "id")
    override val id: Long = 0,

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    val role: AdminMemberRole,

    @Column(name = "email")
    val email: String,

    @Column(name = "pw")
    val password: String,

    @Column(name = "provider")
    @Enumerated(EnumType.STRING)
    val provider: AdminProvider,

    @Column(name = "provider_key")
    val providerKey: String,
) : AdminBaseEntity() {

    val isAdmin: Boolean
        get() = role == AdminMemberRole.ADMIN
}
