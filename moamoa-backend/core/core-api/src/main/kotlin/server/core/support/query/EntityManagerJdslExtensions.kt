package server.core.support.query

import com.linecorp.kotlinjdsl.querymodel.jpql.JpqlQuery
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderer
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery

fun <T : Any> EntityManager.createJdslQuery(
    query: JpqlQuery<*>,
    resultClass: Class<T>,
    offset: Int,
    limit: Int,
): TypedQuery<T> {
    val rendered = JpqlRenderer().render(query, JpqlRenderContext())
    val typedQuery = createQuery(rendered.query, resultClass)
    rendered.params.forEach { (name, value) -> typedQuery.setParameter(name, value) }
    typedQuery.firstResult = offset
    typedQuery.maxResults = limit
    return typedQuery
}
