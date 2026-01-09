package server.feature.member.domain

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
    val role: MemberRole,

    @Column("email")
    val email: String,

    @Column("pw")
    val password: String
) : BaseEntity()
