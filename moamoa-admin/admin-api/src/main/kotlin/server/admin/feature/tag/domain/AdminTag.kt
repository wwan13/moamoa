package server.admin.feature.tag.domain

import jakarta.persistence.Entity
import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.Table
import server.admin.support.domain.AdminBaseEntity

@Entity
@Table(name = "tag")
internal class AdminTag(
    @Id
    @Column(name = "id")
    override val id: Long = 0,

    @Column(name = "title")
    val title: String
) : AdminBaseEntity()
