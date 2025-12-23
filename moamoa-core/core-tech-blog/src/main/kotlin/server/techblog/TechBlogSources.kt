package server.techblog

import org.springframework.stereotype.Component

@Component
class TechBlogSources(
    techBlogSourceByKey: Map<String, TechBlogSource>
) {

    private val sourcesByLowerKey: Map<String, TechBlogSource> = techBlogSourceByKey
        .mapKeys { (key, _) -> key.lowercase() }

    fun get(key: String): TechBlogSource {
        return sourcesByLowerKey[key.lowercase() + SUFFIX]
            ?: throw IllegalArgumentException("존재하지 않는 tech blog 입니다.")
    }

    fun exists(key: String): Boolean = sourcesByLowerKey.containsKey(key.lowercase() + SUFFIX)

    fun validateExists(key: String) {
        if (!exists(key)) {
            throw IllegalArgumentException("tech blog source가 존재하지 않습니다.")
        }
    }

    companion object {
        const val SUFFIX = "source"
    }
}