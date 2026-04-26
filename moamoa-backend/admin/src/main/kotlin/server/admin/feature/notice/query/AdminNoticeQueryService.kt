package server.admin.feature.notice.query

import com.linecorp.kotlinjdsl.dsl.jpql.*
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.admin.feature.notice.domain.AdminNotice
import server.admin.support.query.createJdslQuery

@Service
@Transactional(readOnly = true)
internal class AdminNoticeQueryService(
    @PersistenceContext
    private val entityManager: EntityManager,
) {
    fun findByConditions(conditions: AdminNoticeQueryConditions): AdminNoticeList {
        val size = conditions.size?.takeIf { it > 0 } ?: 20L
        val page = conditions.page?.takeIf { it > 0 } ?: 1L
        val totalCount = fetchTotalCount(conditions)
        val totalPages = if (totalCount == 0L) 0L else (totalCount + size - 1L) / size
        val offset = (page - 1L) * size

        return AdminNoticeList(
            meta = AdminNoticeListMeta(
                page = page,
                size = size,
                totalCount = totalCount,
                totalPages = totalPages,
            ),
            notices = fetchNotices(conditions, size, offset),
        )
    }

    private fun fetchNotices(
        conditions: AdminNoticeQueryConditions,
        size: Long,
        offset: Long,
    ): List<AdminNoticeSummary> {
        val keyword = conditions.query?.takeIf { it.isNotBlank() }?.let { "%$it%" }
        val jpqlQuery = jpql {
            selectNew<AdminNoticeSummary>(
                path(AdminNotice::id),
                path(AdminNotice::title),
                path(AdminNotice::chip),
                path(AdminNotice::content),
                path(AdminNotice::published),
                path(AdminNotice::publishedAt),
            )
                .from(entity(AdminNotice::class))
                .whereAnd(
                    keyword?.let {
                        or(
                            path(AdminNotice::title).like(it),
                            path(AdminNotice::chip).like(it),
                            path(AdminNotice::content).like(it),
                        )
                    },
                    conditions.published?.let { path(AdminNotice::published).equal(it) },
                )
                .orderBy(
                    path(AdminNotice::publishedAt).desc(),
                    path(AdminNotice::id).desc(),
                )
        }

        return entityManager
            .createJdslQuery(
                query = jpqlQuery,
                resultClass = AdminNoticeSummary::class.java,
                offset = offset.toInt(),
                limit = size.toInt(),
            )
            .resultList
    }

    private fun fetchTotalCount(conditions: AdminNoticeQueryConditions): Long {
        val keyword = conditions.query?.takeIf { it.isNotBlank() }?.let { "%$it%" }
        val jpqlQuery = jpql {
            select(count(AdminNotice::id))
                .from(entity(AdminNotice::class))
                .whereAnd(
                    keyword?.let {
                        or(
                            path(AdminNotice::title).like(it),
                            path(AdminNotice::chip).like(it),
                            path(AdminNotice::content).like(it),
                        )
                    },
                    conditions.published?.let { path(AdminNotice::published).equal(it) },
                )
        }

        return entityManager
            .createJdslQuery(
                query = jpqlQuery,
                resultClass = Long::class.javaObjectType,
                offset = 0,
                limit = Int.MAX_VALUE,
            )
            .resultList
            .firstOrNull()
            ?: 0L
    }
}
