package server.domain.posttag

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.domain.BaseEntity

@Table(name = "post_tag")
data class PostTag(
    @Id
    @Column("id")
    override val id: Long = 0,

    @Column("post_id")
    val postId: Long,

    @Column("tag_id")
    val tagId: Long,
) : BaseEntity()
