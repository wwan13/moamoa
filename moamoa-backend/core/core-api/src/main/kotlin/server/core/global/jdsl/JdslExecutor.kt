package server.core.global.jdsl

import com.linecorp.kotlinjdsl.querymodel.jpql.JpqlQuery
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderer
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.TypedQuery
import org.springframework.stereotype.Component
import kotlin.collections.component1
import kotlin.collections.component2

@Component
class JdslExecutor(
    @PersistenceContext
    private val entityManager: EntityManager,
) {

    fun <T : Any> createQuery(
        query: JpqlQuery<*>,
        resultClass: Class<T>,
        offset: Int,
        limit: Int,
    ): TypedQuery<T> {
        val rendered = JpqlRenderer().render(query, JpqlRenderContext())
        val typedQuery = entityManager.createQuery(rendered.query, resultClass)
        rendered.params.forEach { (name, value) -> typedQuery.setParameter(name, value) }
        typedQuery.firstResult = offset
        typedQuery.maxResults = limit
        return typedQuery
    }
}
