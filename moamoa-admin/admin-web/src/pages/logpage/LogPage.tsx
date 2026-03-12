import { useMemo, useState } from "react"
import PageTitle from "../../components/pagetitle/PageTitle.tsx"
import { Dropdown } from "../../components/ui/Dropdown.tsx"
import { Search } from "../../components/ui/Search.tsx"
import { useLogsQuery } from "../../queries/log.queries"
import type { AdminLogSummary } from "../../api/log.api"
import styles from "./LogPage.module.css"

const logLevelOptions = [
    { value: "ALL", label: "모든 레벨" },
    { value: "TRACE", label: "TRACE" },
    { value: "DEBUG", label: "DEBUG" },
    { value: "INFO", label: "INFO" },
    { value: "WARN", label: "WARN" },
    { value: "ERROR", label: "ERROR" },
]

const logTypeOptions = [
    { value: "ALL", label: "모든 타입" },
    { value: "REQUEST", label: "REQUEST" },
    { value: "ERROR", label: "ERROR" },
    { value: "REQ", label: "REQ" },
    { value: "BIZ", label: "BIZ" },
    { value: "DB", label: "DB" },
    { value: "REDIS", label: "REDIS" },
    { value: "API", label: "API" },
    { value: "REQ_ERROR", label: "REQ_ERROR" },
    { value: "WORKER", label: "WORKER" },
]

const pollingOptions = [
    { value: "5000", label: "5초 (기본)" },
    { value: "10000", label: "10초" },
    { value: "30000", label: "30초" },
    { value: "60000", label: "60초" },
    { value: "0", label: "폴링 끄기" },
]

const sizeOptions = [
    { value: "50", label: "50개" },
    { value: "100", label: "100개" },
]

const normalizeFilterValue = (value: string): string | undefined => {
    return value === "ALL" ? undefined : value
}

const parseLogData = (raw: string): unknown => {
    if (!raw) return ""
    try {
        return JSON.parse(raw)
    } catch {
        return raw
    }
}

const toPrettyLogBlock = (log: AdminLogSummary): string => {
    const payload = {
        timestamp: log.timestamp,
        level: log.logLevel,
        type: log.type,
        traceId: log.traceId,
        logger: log.loggerName,
        message: log.message,
        data: parseLogData(log.data),
        service: log.service,
    }

    return JSON.stringify(payload, null, 2)
}

const LogPage = () => {
    const [searchInput, setSearchInput] = useState("")
    const [traceId, setTraceId] = useState("")
    const [logLevel, setLogLevel] = useState("ALL")
    const [logType, setLogType] = useState("ALL")
    const [pollingMs, setPollingMs] = useState("5000")
    const [size, setSize] = useState("100")

    const queryConditions = useMemo(
        () => ({
            logLevel: normalizeFilterValue(logLevel),
            type: normalizeFilterValue(logType),
            traceId: traceId || undefined,
            size: Number(size),
        }),
        [logLevel, logType, traceId, size]
    )

    const { data, isLoading, isFetching } = useLogsQuery(
        queryConditions,
        Number(pollingMs)
    )

    const logs = useMemo(() => {
        const items = data?.items ?? []
        return [...items].reverse()
    }, [data?.items])

    return (
        <div className={styles.wrap}>
            <PageTitle value="시스템 로그" />

            <section className={styles.filters}>
                <div className={styles.left}>
                    <div className={styles.searchWrap}>
                        <Search
                            value={searchInput}
                            placeholder="traceId 검색"
                            onChange={setSearchInput}
                            onSearch={(value) => {
                                setTraceId(value.trim())
                            }}
                        />
                    </div>
                    <Dropdown
                        options={logLevelOptions}
                        value={logLevel}
                        onChange={(value) => setLogLevel(value)}
                    />
                    <Dropdown
                        options={logTypeOptions}
                        value={logType}
                        onChange={(value) => setLogType(value)}
                    />
                </div>

                <div className={styles.right}>
                    <Dropdown
                        options={sizeOptions}
                        value={size}
                        onChange={(value) => setSize(value)}
                    />
                    <Dropdown
                        options={pollingOptions}
                        value={pollingMs}
                        onChange={(value) => setPollingMs(value)}
                    />
                </div>
            </section>

            <section className={styles.statusRow}>
                <span>총 {logs.length}개 로그</span>
                <span className={isFetching ? styles.refreshing : ""}>
                    {isFetching ? "갱신 중..." : "최신 상태"}
                </span>
            </section>

            <section className={styles.viewerWrap}>
                <div className={styles.viewerHeader}>Log</div>
                <div className={styles.viewport}>
                    {isLoading && <div className={styles.empty}>불러오는 중...</div>}
                    {!isLoading && logs.length === 0 && (
                        <div className={styles.empty}>조회된 로그가 없습니다.</div>
                    )}

                    {!isLoading &&
                        logs.map((log) => (
                            <article key={log.id} className={styles.logBlock}>
                                <pre className={styles.logPre}>{toPrettyLogBlock(log)}</pre>
                            </article>
                        ))}
                </div>
            </section>
        </div>
    )
}

export default LogPage
