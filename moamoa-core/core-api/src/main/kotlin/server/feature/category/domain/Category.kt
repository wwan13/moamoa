package server.feature.category.domain

enum class Category(
    val id: Long,
    val title: String
) {
    ENGINEERING(10L, "백앤드"),
    PRODUCT(20L, "프로덕트"),
    DESIGN(30L, "디자인"),
    ETC(40L, "기타"),

    UNDEFINED(999L, "");

    companion object {
        val validCategories: List<Category>
            get() = Category.entries.filter { it != UNDEFINED }

        fun fromId(id: Long) = validCategories.firstOrNull { it.id == id }
            ?: throw IllegalArgumentException("존재하지 않는 카테고리입니다.")

        fun fromName(name: String) = validCategories.firstOrNull { it.name == name }
            ?: throw IllegalArgumentException("존재하지 않는 카테고리입니다.")
    }
}