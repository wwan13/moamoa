package server.admin.feature.posttag.domain

import jakarta.persistence.Entity
import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.Table
import server.admin.support.domain.AdminBaseEntity

@Entity
@Table(name = "post_tag")
internal class AdminPostTag(
    @Id
    @Column(name = "id")
    override val id: Long = 0,

    @Column(name = "post_id")
    val postId: Long,

    @Column(name = "tag_id")
    val tagId: Long,
) : AdminBaseEntity()
