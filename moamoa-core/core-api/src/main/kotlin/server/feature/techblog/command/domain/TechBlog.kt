package server.feature.techblog.command.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import support.domain.BaseEntity

@Entity
@Table(
    name = "tech_blog",
    uniqueConstraints = [
        UniqueConstraint(name = "UK3ryw1jcyeyug2bgvouo9fuv8l", columnNames = ["title"]),
        UniqueConstraint(name = "UKlkrp410nrwg3dj8dao6a1gcok", columnNames = ["tech_blog_key"])
    ]
)
class TechBlog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    override val id: Long = 0,

    @Column(name = "title", length = 255, nullable = false)
    val title: String,

    @Column(name = "tech_blog_key", length = 255, nullable = false)
    val key: String,

    @Column(name = "blog_url", length = 1024, nullable = false)
    val blogUrl: String,

    @Column(name = "icon", length = 255, nullable = false)
    val icon: String,

    subscriptionCount: Long = 0
) : BaseEntity() {
    @Column(name = "subscription_count", nullable = false)
    var subscriptionCount: Long = subscriptionCount
        private set

    fun updateSubscriptionCount(delta: Long) {
        subscriptionCount = (subscriptionCount + delta).coerceAtLeast(0)
    }
}
