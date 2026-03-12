import { type UIEvent, useEffect, useMemo, useRef, useState } from "react"
import PageTitle from "../../components/pagetitle/PageTitle.tsx"
import { Dropdown } from "../../components/ui/Dropdown.tsx"
import { Search } from "../../components/ui/Search.tsx"
import { useInfiniteLogsQuery } from "../../queries/log.queries"
import type { AdminLogSummary } from "../../api/log.api"
import Button from "../../components/ui/Button.tsx"
import styles from "./LogPage.module.css"

const logLevelOptions = [
    { value: "ALL", label: "LOG LEVEL" },
    { value: "TRACE", label: "TRACE" },
    { value: "DEBUG", label: "DEBUG" },
    { value: "INFO", label: "INFO" },
    { value: "WARN", label: "WARN" },
    { value: "ERROR", label: "ERROR" },
]

const logTypeOptions = [
    { value: "ALL", label: "TYPE" },
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
    { value: "5000", label: "5s" },
    { value: "10000", label: "10s" },
    { value: "30000", label: "30s" },
    { value: "60000", label: "60s" },
    { value: "0", label: "off" },
]

const sizeOptions = [
    { value: "50", label: "50" },
    { value: "100", label: "100" },
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
    const viewportRef = useRef<HTMLDivElement | null>(null)
    const hasInitialScrolledRef = useRef(false)
    const topLoadingRef = useRef(false)

    const queryConditions = useMemo(
        () => ({
            logLevel: normalizeFilterValue(logLevel),
            type: normalizeFilterValue(logType),
            traceId: traceId || undefined,
            size: Number(size),
        }),
        [logLevel, logType, traceId, size]
    )

    const {
        data,
        isLoading,
        refetch,
        fetchNextPage,
        hasNextPage,
        isFetchingNextPage,
    } = useInfiniteLogsQuery(
        queryConditions,
        Number(pollingMs)
    )

    const logs = useMemo(() => {
        const pages = data?.pages ?? []
        return [...pages]
            .reverse()
            .flatMap((page) => [...page.items].reverse())
    }, [data?.pages])

    const scrollToBottom = () => {
        const viewport = viewportRef.current
        if (!viewport) return
        viewport.scrollTop = viewport.scrollHeight
    }

    const loadOlderLogs = async () => {
        const viewport = viewportRef.current
        if (!viewport) return
        if (topLoadingRef.current) return
        if (!hasNextPage || isFetchingNextPage) return

        topLoadingRef.current = true
        const previousScrollHeight = viewport.scrollHeight
        const previousScrollTop = viewport.scrollTop

        await fetchNextPage()

        requestAnimationFrame(() => {
            const currentViewport = viewportRef.current
            if (!currentViewport) {
                topLoadingRef.current = false
                return
            }

            const addedHeight = currentViewport.scrollHeight - previousScrollHeight
            currentViewport.scrollTop = previousScrollTop + addedHeight
            topLoadingRef.current = false
        })
    }

    const handleViewportScroll = (event: UIEvent<HTMLDivElement>) => {
        const viewport = event.currentTarget
        if (viewport.scrollTop > 16) return
        void loadOlderLogs()
    }

    useEffect(() => {
        if (hasInitialScrolledRef.current) return
        if (logs.length === 0) return

        scrollToBottom()
        hasInitialScrolledRef.current = true
    }, [logs])

    useEffect(() => {
        hasInitialScrolledRef.current = false
    }, [queryConditions])

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
                <div className={styles.statusRight}>
                    <Button
                        type="button"
                        variant="outline"
                        onClick={async () => {
                            await refetch()
                            scrollToBottom()
                        }}
                    >
                        refresh
                    </Button>
                </div>
            </section>

            <section className={styles.viewerWrap}>
                <div className={styles.viewerHeader}>Log</div>
                <div
                    ref={viewportRef}
                    className={styles.viewport}
                    onScroll={handleViewportScroll}
                >
                    {isFetchingNextPage && (
                        <div className={styles.loadMore}>옛날 로그 불러오는 중...</div>
                    )}
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
