package server.admin.feature.tag.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.admin.domain.AdminBaseEntity

@Table(name = "tag")
internal data class AdminTag(
    @Id
    @Column("id")
    override val id: Long = 0,

    @Column("title")
    val title: String
) : AdminBaseEntity()
