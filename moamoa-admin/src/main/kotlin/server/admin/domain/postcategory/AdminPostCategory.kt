package server.admin.domain.postcategory

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.admin.domain.BaseEntity

@Table(name = "post_category")
data class AdminPostCategory(
    @Id
    @Column("id")
    override val id: Long = 0,

    @Column("post_id")
    val postId: Long,

    @Column("category_id")
    val categoryId: Long,
) : BaseEntity()
