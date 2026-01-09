package server.feature.tag.application

import server.feature.tag.domain.Tag

data class TagData(
    val id: Long,
    val title: String
) {
    constructor(tag: Tag) : this(
        id = tag.id,
        title = tag.title
    )
}