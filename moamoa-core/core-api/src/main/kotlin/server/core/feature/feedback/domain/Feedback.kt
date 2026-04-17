package server.core.feature.feedback.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import server.core.support.domain.BaseEntity

@Entity
@Table(name = "feedback")
class Feedback(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    override val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 100, nullable = false)
    val type: FeedbackType,

    @Column(name = "content", length = 1000, nullable = false)
    val content: String,

    @Column(name = "email", length = 255, nullable = false)
    val email: String,

    @Column(name = "answer", columnDefinition = "TEXT")
    val answer: String = "",
) : BaseEntity()
