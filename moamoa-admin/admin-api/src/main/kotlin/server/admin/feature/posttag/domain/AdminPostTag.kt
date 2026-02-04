package server.admin.feature.posttag.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.admin.domain.AdminBaseEntity

@Table(name = "post_tag")
internal data class AdminPostTag(
    @Id
    @Column("id")
    override val id: Long = 0,

    @Column("post_id")
    val postId: Long,

    @Column("tag_id")
    val tagId: Long,
) : AdminBaseEntity()
