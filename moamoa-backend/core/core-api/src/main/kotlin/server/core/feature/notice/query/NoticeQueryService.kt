package server.core.feature.notice.query

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.notice.domain.Notice
import server.core.global.jdsl.JdslExecutor
import server.core.support.domain.ListEntry
import server.core.support.paging.Paging
import server.core.support.paging.calculateTotalPage

@Service
class NoticeQueryService(
    private val jdslExecutor: JdslExecutor,
) {

    @Transactional(readOnly = true)
    fun findByConditions(conditions: NoticeQueryConditions): NoticeList {
        val paging = Paging(
            size = conditions.size ?: 20,
            page = conditions.page ?: 1,
        )

        val entry = fetchEntry(paging, conditions.query)
        val totalCount = entry.count

        return NoticeList(
            meta = NoticeListMeta(
                page = paging.page,
                size = paging.size,
                totalCount = totalCount,
                totalPages = calculateTotalPage(totalCount, paging.size),
            ),
            notices = entry.list,
        )
    }

    private fun fetchEntry(paging: Paging, query: String?): ListEntry<NoticeSummary> {
        val count = fetchCount(query)
        val list = fetchNotices(paging, query)

        return ListEntry(
            count = count,
            list = list,
        )
    }

    private fun fetchNotices(paging: Paging, query: String?): List<NoticeSummary> {
        val limit = paging.size.toInt()
        val offset = (paging.page - 1L) * paging.size
        val jpqlQuery = createNoticesQuery(query)

        return jdslExecutor
            .createQuery(
                query = jpqlQuery,
                resultClass = NoticeSummary::class.java,
                offset = offset.toInt(),
                limit = limit,
            )
            .resultList
    }

    private fun fetchCount(query: String?): Long {
        val jpqlQuery = createCountQuery(query)

        return jdslExecutor
            .createQuery(
                query = jpqlQuery,
                resultClass = Long::class.javaObjectType,
                offset = 0,
                limit = Int.MAX_VALUE,
            )
            .resultList
            .firstOrNull()
            ?: 0L
    }

    private fun createNoticesQuery(query: String?) = jpql {
        val keyword = query?.takeIf { it.isNotBlank() }?.let { "%$it%" }

        selectNew<NoticeSummary>(
            path(Notice::id),
            path(Notice::title),
            path(Notice::chip),
            path(Notice::content),
            path(Notice::publishedAt),
        )
            .from(entity(Notice::class))
            .whereAnd(
                path(Notice::published).equal(true),
                keyword?.let {
                    or(
                        path(Notice::title).like(it),
                        path(Notice::content).like(it),
                        path(Notice::chip).like(it),
                    )
                },
            )
            .orderBy(path(Notice::publishedAt).desc())
    }

    private fun createCountQuery(query: String?) = jpql {
        val keyword = query?.takeIf { it.isNotBlank() }?.let { "%$it%" }

        select(count(Notice::id))
            .from(entity(Notice::class))
            .whereAnd(
                path(Notice::published).equal(true),
                keyword?.let {
                    or(
                        path(Notice::title).like(it),
                        path(Notice::content).like(it),
                        path(Notice::chip).like(it),
                    )
                },
            )
    }
}
