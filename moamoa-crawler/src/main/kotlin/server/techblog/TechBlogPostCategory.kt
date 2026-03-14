package server.techblog

enum class TechBlogPostCategory(val categoryId: Long, val title: String) {
    ENGINEERING(10L, "ENGINEERING"),
    PRODUCT(20L, "PRODUCT"),
    DESIGN(30L, "DESIGN"),
    ETC(40L, "ETC"),
    UNDEFINED(999L, "")
}
