package server.admin.feature.member.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.admin.domain.AdminBaseEntity

@Table("member")
internal data class AdminMember(
    @Id
    @Column("id")
    override val id: Long = 0,

    @Column("role")
    val role: AdminMemberRole = AdminMemberRole.USER,

    @Column("email")
    val email: String,

    @Column("pw")
    val password: String,

    @Column("provider")
    val provider: AdminProvider,

    @Column("provider_key")
    val providerKey: String,
) : AdminBaseEntity()
