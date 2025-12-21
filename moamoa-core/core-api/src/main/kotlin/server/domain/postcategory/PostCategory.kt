package server.domain.postcategory

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.domain.BaseEntity

@Table(name = "post_category")
data class PostCategory(
    @Id
    @Column("id")
    override val id: Long = 0,

    @Column("post_id")
    val postId: Long,

    @Column("category_id")
    val categoryId: Long,
) : BaseEntity()
