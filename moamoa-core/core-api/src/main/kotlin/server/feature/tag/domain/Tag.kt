package server.feature.tag.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.domain.BaseEntity

@Table(name = "tag")
data class Tag(
    @Id
    @Column("id")
    override val id: Long = 0,

    @Column("title")
    val title: String,
) : BaseEntity()