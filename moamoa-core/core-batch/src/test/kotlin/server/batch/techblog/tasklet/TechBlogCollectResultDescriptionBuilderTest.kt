package server.batch.techblog.tasklet

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import server.batch.techblog.monitoring.FetchStatus
import server.batch.techblog.monitoring.NotifyResultType
import server.batch.techblog.monitoring.TechBlogCollectMonitorSnapshot
import server.batch.techblog.monitoring.TechBlogCollectSourceResult
import server.batch.techblog.monitoring.TechBlogCollectTotals
import test.UnitTest

class TechBlogCollectResultDescriptionBuilderTest : UnitTest() {

    @Test
    fun `실패와 성공이 함께 있으면 FAIL 섹션이 먼저 나오고 title 기준으로 출력한다`() {
        val snapshot = snapshot(
            sources = listOf(
                source(title = "실패블로그", status = FetchStatus.FAILED, added = 1, errorType = "Timeout", errorMessage = "upstream"),
                source(title = "성공블로그", status = FetchStatus.SUCCESS, added = 2),
            )
        )
        val description = TechBlogCollectResultDescriptionBuilder.build(summary(snapshot), snapshot)

        description.contains("FAIL") shouldBe true
        description.contains("SUCCESS") shouldBe true
        val isFailBeforeSuccess = description.indexOf("FAIL") < description.indexOf("SUCCESS")
        isFailBeforeSuccess shouldBe true
        description.contains("실패블로그") shouldBe true
        description.contains("성공블로그") shouldBe true
        description.contains("techBlogKey") shouldBe false
        val detailLines = description.lines().filter { it.startsWith("- ") }
        detailLines.all { !it.contains("fetched=") } shouldBe true
    }

    @Test
    fun `실패가 없으면 FAIL 섹션을 출력하지 않는다`() {
        val snapshot = snapshot(
            sources = listOf(
                source(title = "성공A", status = FetchStatus.SUCCESS, added = 1),
                source(title = "성공B", status = FetchStatus.SUCCESS, added = 0),
            )
        )

        val description = TechBlogCollectResultDescriptionBuilder.build(summary(snapshot), snapshot)

        description.contains("모든 tech blog fetch가 성공했습니다.") shouldBe true
        description.contains("FAIL") shouldBe false
        description.contains("SUCCESS") shouldBe false
        description.contains("ADDED") shouldBe true
        description.contains("성공A: added=1") shouldBe true
        description.contains("성공B: added=0") shouldBe false
    }

    @Test
    fun `성공이 없으면 SUCCESS 섹션을 출력하지 않는다`() {
        val snapshot = snapshot(
            sources = listOf(
                source(title = "실패A", status = FetchStatus.FAILED, added = 0, errorType = "IllegalState", errorMessage = "boom"),
            )
        )

        val description = TechBlogCollectResultDescriptionBuilder.build(summary(snapshot), snapshot)

        description.contains("FAIL") shouldBe true
        description.contains("SUCCESS") shouldBe false
    }

    @Test
    fun `길이 제한을 초과하면 3500자로 자르고 생략 개수를 표시한다`() {
        val manySources = (1..400).map {
            source(title = "실패블로그-$it-" + "x".repeat(20), status = FetchStatus.FAILED, added = it, errorType = "Timeout", errorMessage = "m".repeat(30))
        }
        val snapshot = snapshot(sources = manySources)

        val description = TechBlogCollectResultDescriptionBuilder.build(summary(snapshot), snapshot)

        (description.length <= 3500) shouldBe true
        description.contains("... and ") shouldBe true
        description.contains("runId=") shouldBe true
    }

    @Test
    fun `모두 성공이면서 추가 게시글이 없으면 성공 메시지만 노출한다`() {
        val snapshot = snapshot(
            sources = listOf(
                source(title = "성공A", status = FetchStatus.SUCCESS, added = 0),
                source(title = "성공B", status = FetchStatus.SUCCESS, added = 0),
            )
        )

        val description = TechBlogCollectResultDescriptionBuilder.build(summary(snapshot), snapshot)

        description.contains("모든 tech blog fetch가 성공했습니다.") shouldBe true
        description.contains("ADDED") shouldBe false
        description.lines().any { it.startsWith("- ") } shouldBe false
    }

    @Test
    fun `에러 설명은 error type과 message를 포함한다`() {
        val snapshot = snapshot(
            sources = listOf(
                source(title = "실패A", status = FetchStatus.FAILED, added = 0, errorType = "Timeout", errorMessage = "socket timeout"),
                source(title = "실패B", status = FetchStatus.FAILED, added = 0, errorType = "IllegalState", errorMessage = "boom"),
            )
        )
        val failed = snapshot.sources.filter { it.fetchStatus == FetchStatus.FAILED }

        val description = TechBlogCollectErrorDescriptionBuilder.build(snapshot, failed)

        description.contains("FAILED SOURCES") shouldBe true
        description.contains("실패A: Timeout:socket timeout") shouldBe true
        description.contains("실패B: IllegalState:boom") shouldBe true
    }

    @Test
    fun `에러 설명이 길면 3500자 제한과 생략 개수 표기를 지킨다`() {
        val manyFailed = (1..400).map {
            source(
                title = "실패-$it",
                status = FetchStatus.FAILED,
                added = 0,
                errorType = "Timeout",
                errorMessage = "m".repeat(30)
            )
        }
        val snapshot = snapshot(sources = manyFailed)
        val failed = snapshot.sources.filter { it.fetchStatus == FetchStatus.FAILED }

        val description = TechBlogCollectErrorDescriptionBuilder.build(snapshot, failed)

        (description.length <= 3500) shouldBe true
        description.contains("... and ") shouldBe true
    }

    private fun summary(snapshot: TechBlogCollectMonitorSnapshot): String = listOf(
        "runId=${snapshot.collectRunId}",
        "collectExecutedAt=${snapshot.collectExecutedAtMillis}",
        "source=${snapshot.totals.sourceCount}",
        "success=${snapshot.totals.successCount}",
        "failed=${snapshot.totals.failureCount}",
        "fetched=${snapshot.totals.fetchedPostCount}",
        "added=${snapshot.totals.addedPostCount}",
    ).joinToString(", ")

    private fun snapshot(sources: List<TechBlogCollectSourceResult>): TechBlogCollectMonitorSnapshot {
        val successCount = sources.count { it.fetchStatus == FetchStatus.SUCCESS }
        val failureCount = sources.count { it.fetchStatus == FetchStatus.FAILED }

        return TechBlogCollectMonitorSnapshot(
            collectRunId = 100L,
            collectExecutedAtMillis = 100L,
            collectDateKst = "2026-02-06",
            updatedAtMillis = 100L,
            sources = sources,
            totals = TechBlogCollectTotals(
                sourceCount = sources.size,
                successCount = successCount,
                failureCount = failureCount,
                fetchedPostCount = sources.sumOf { it.fetchedPostCount },
                addedPostCount = sources.sumOf { it.addedPostCount },
            ),
            lastNotifyRunId = null,
            lastNotifiedAtMillis = null,
            lastNotifyResultType = NotifyResultType.RESULT,
        )
    }

    private fun source(
        title: String,
        status: FetchStatus,
        added: Int,
        errorType: String? = null,
        errorMessage: String? = null,
    ): TechBlogCollectSourceResult = TechBlogCollectSourceResult(
        techBlogId = title.hashCode().toLong(),
        techBlogKey = title.lowercase(),
        techBlogTitle = title,
        fetchStatus = status,
        fetchedPostCount = 10,
        addedPostCount = added,
        errorType = errorType,
        errorMessage = errorMessage,
    )
}
