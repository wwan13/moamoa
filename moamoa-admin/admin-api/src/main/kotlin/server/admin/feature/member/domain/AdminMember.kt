package server.admin.feature.member.domain

import jakarta.persistence.Entity
import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.Table
import support.admin.domain.AdminBaseEntity

@Entity
@Table(name = "member")
internal class AdminMember(
    @Id
    @Column(name = "id")
    override val id: Long = 0,

    @Column(name = "role")
    val role: AdminMemberRole = AdminMemberRole.USER,

    @Column(name = "email")
    val email: String,

    @Column(name = "pw")
    val password: String,

    @Column(name = "provider")
    val provider: AdminProvider,

    @Column(name = "provider_key")
    val providerKey: String,
) : AdminBaseEntity() {

    val isAdmin: Boolean
        get() = role == AdminMemberRole.ADMIN
}
