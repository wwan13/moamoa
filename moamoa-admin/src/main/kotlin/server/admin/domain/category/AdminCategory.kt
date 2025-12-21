package server.admin.domain.category

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.admin.domain.BaseEntity

@Table(name = "category")
data class AdminCategory(
    @Id
    @Column("id")
    override val id: Long = 0,

    @Column("title")
    val title: String
) : BaseEntity()