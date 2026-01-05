package server.application

import server.domain.tag.Tag

data class TagData(
    val id: Long,
    val title: String
) {
    constructor(tag: Tag) : this(
        id = tag.id,
        title = tag.title
    )
}