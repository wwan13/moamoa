package server.batch.post.processor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import server.batch.post.dto.PostCategory
import server.batch.post.dto.PostSummary
import server.batch.post.dto.PreCategorizingPostResult

@Component
internal class PreCategorizingPostProcessor(
    private val objectMapper: ObjectMapper
) : ItemProcessor<List<PostSummary>, PreCategorizingPostResult> {

    private val categoryIdByTitle = linkedMapOf(
        "BACKEND" to 1L,
        "FRONTEND" to 2L,
        "PRODUCT" to 3L,
        "DESIGN" to 4L
    )

    private val categoryTagsByTitle: Map<String, Set<String>> = loadCategoryTags()

    override fun process(items: List<PostSummary>): PreCategorizingPostResult {
        val categorized = mutableListOf<PostCategory>()
        val uncategorized = mutableListOf<PostSummary>()

        items.forEach { item ->
            val categoryId = matchCategoryIdByTagsAndText(item)
            if (categoryId == null) {
                uncategorized.add(item)
            } else {
                categorized.add(PostCategory(postId = item.postId, categoryId = categoryId))
            }
        }

        return PreCategorizingPostResult(
            categorized = categorized,
            uncategorized = uncategorized
        )
    }

    private fun matchCategoryIdByTagsAndText(summary: PostSummary): Long? {
        val normalizedTags = summary.tags.asSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toList()

        val normalizedText = sequenceOf(summary.title, summary.description)
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .joinToString(" ")

        if (normalizedTags.isEmpty() && normalizedText.isBlank()) return null

        var bestCategoryTitle: String? = null
        var bestCount = 0
        var hasTie = false

        categoryTagsByTitle.forEach { (categoryTitle, tagSet) ->
            val tagScore = normalizedTags.count { it in tagSet }
            val textScore = if (normalizedText.isBlank()) 0 else tagSet.count { normalizedText.contains(it) }
            val count = tagScore + textScore
            if (count <= 0) return@forEach

            when {
                count > bestCount -> {
                    bestCount = count
                    bestCategoryTitle = categoryTitle
                    hasTie = false
                }
                count == bestCount -> {
                    hasTie = true
                }
            }
        }

        if (bestCount == 0 || hasTie) return null
        return bestCategoryTitle?.let { categoryIdByTitle[it] }
    }

    private fun loadCategoryTags(): Map<String, Set<String>> {
        val resource = javaClass.classLoader.getResourceAsStream("statics/categories.json")
            ?: throw IllegalStateException("categories.json not found in classpath")

        val raw: Map<String, List<String>> = resource.use { objectMapper.readValue(it) }

        return raw.mapNotNull { (title, tags) ->
            if (!categoryIdByTitle.containsKey(title)) return@mapNotNull null
            title to tags.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        }.toMap()
    }
}
