import { type UIEvent, useEffect, useMemo, useRef, useState } from "react"
import PageTitle from "../../components/pagetitle/PageTitle.tsx"
import { DatePicker } from "../../components/ui/DatePicker.tsx"
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
    { value: "BIZ", label: "BIZ" },
    { value: "DB", label: "DB" },
    { value: "REDIS", label: "REDIS" },
    { value: "EVENT", label: "EVENT" },
]

const traceIdModeOptions = [
    { value: "ALL", label: "TRACE" },
    { value: "SYSTEM", label: "SYSTEM" },
    { value: "REQUEST", label: "REQUEST" },
]

const logPeriodOptions = [
    { value: "10m", label: "10분" },
    { value: "30m", label: "30분" },
    { value: "1h", label: "1시간" },
    { value: "1d", label: "1일" },
    { value: "3d", label: "3일" },
    { value: "7d", label: "7일" },
    { value: "custom", label: "범위지정" },
]

type LogPeriodOptionValue = "10m" | "30m" | "1h" | "1d" | "3d" | "7d" | "custom"

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

const LOOKBACK_MINUTES_MAP: Record<Exclude<LogPeriodOptionValue, "custom">, number> = {
    "10m": 10,
    "30m": 30,
    "1h": 60,
    "1d": 24 * 60,
    "3d": 3 * 24 * 60,
    "7d": 7 * 24 * 60,
}

const MAX_LOOKBACK_DAYS = 7

const normalizeFilterValue = (value: string): string | undefined => {
    return value === "ALL" ? undefined : value
}

const pad = (value: number): string => value.toString().padStart(2, "0")

const formatForDatetimeInput = (date: Date): string => {
    return [
        `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`,
        `${pad(date.getHours())}:${pad(date.getMinutes())}`,
    ].join("T")
}

const formatForApi = (date: Date): string => {
    return `${formatForDatetimeInput(date)}:${pad(date.getSeconds())}`
}

const getMaxLookbackStart = (base: Date): Date => {
    const lookbackStart = new Date(base)
    lookbackStart.setHours(0, 0, 0, 0)
    lookbackStart.setDate(lookbackStart.getDate() - MAX_LOOKBACK_DAYS)
    return lookbackStart
}

const parseDatetimeInput = (value: string): Date | null => {
    if (!value) return null
    const parsed = new Date(value)
    if (Number.isNaN(parsed.getTime())) return null
    return parsed
}

const toInputValueFromApiTimestamp = (timestamp: string): string => {
    const parsed = new Date(timestamp)
    if (Number.isNaN(parsed.getTime())) return ""
    return formatForDatetimeInput(parsed)
}

const toPresetRange = (
    option: Exclude<LogPeriodOptionValue, "custom">
): { fromTimestamp: string; toTimestamp: string } => {
    const to = new Date()
    const minutes = LOOKBACK_MINUTES_MAP[option]
    const from = new Date(to.getTime() - minutes * 60 * 1000)

    return {
        fromTimestamp: formatForApi(from),
        toTimestamp: formatForApi(to),
    }
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
    }

    return JSON.stringify(payload, null, 2)
}

const formatLastFetchedAt = (timestamp: number): string => {
    if (!timestamp) return "-"
    const date = new Date(timestamp)
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const LogPage = () => {
    const initialRange = useMemo(() => toPresetRange("10m"), [])

    const [searchInput, setSearchInput] = useState("")
    const [traceId, setTraceId] = useState("")
    const [traceIdMode, setTraceIdMode] = useState("ALL")
    const [logLevel, setLogLevel] = useState("ALL")
    const [logType, setLogType] = useState("ALL")
    const [logPeriod, setLogPeriod] = useState<LogPeriodOptionValue>("10m")
    const [pollingMs, setPollingMs] = useState("0")
    const [size, setSize] = useState("100")
    const [fromTimestamp, setFromTimestamp] = useState(initialRange.fromTimestamp)
    const [toTimestamp, setToTimestamp] = useState(initialRange.toTimestamp)
    const [customStart, setCustomStart] = useState("")
    const [customEnd, setCustomEnd] = useState("")

    const viewportRef = useRef<HTMLDivElement | null>(null)
    const hasInitialScrolledRef = useRef(false)
    const topLoadingRef = useRef(false)

    const queryConditions = useMemo(
        () => ({
            logLevel: normalizeFilterValue(logLevel),
            type: normalizeFilterValue(logType),
            traceId: traceId || undefined,
            traceIdMode: normalizeFilterValue(traceIdMode),
            fromTimestamp,
            toTimestamp,
            size: Number(size),
        }),
        [fromTimestamp, logLevel, logType, size, toTimestamp, traceId, traceIdMode]
    )

    const {
        data,
        isLoading,
        isFetching,
        dataUpdatedAt,
        refetch,
        fetchNextPage,
        hasNextPage,
        isFetchingNextPage,
    } = useInfiniteLogsQuery(queryConditions, Number(pollingMs))

    const logs = useMemo(() => {
        const pages = data?.pages ?? []
        return [...pages]
            .reverse()
            .flatMap((page) => [...page.items].reverse())
    }, [data?.pages])
    const isRefreshLoading = isLoading || isFetching || isFetchingNextPage

    const scrollToBottom = () => {
        const viewport = viewportRef.current
        if (!viewport) return
        viewport.scrollTop = viewport.scrollHeight
    }

    const applyPresetRange = (option: Exclude<LogPeriodOptionValue, "custom">) => {
        const presetRange = toPresetRange(option)
        setFromTimestamp(presetRange.fromTimestamp)
        setToTimestamp(presetRange.toTimestamp)
        setCustomStart("")
        setCustomEnd("")
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

    useEffect(() => {
        if (logPeriod !== "custom") return

        const start = parseDatetimeInput(customStart)
        const end = parseDatetimeInput(customEnd)
        if (!start || !end) return

        const now = new Date()
        const maxLookbackStart = getMaxLookbackStart(now)
        if (start < maxLookbackStart) return
        if (end > now) return
        if (start > end) return

        setFromTimestamp(formatForApi(start))
        setToTimestamp(formatForApi(end))
    }, [customEnd, customStart, logPeriod])

    return (
        <div className={styles.wrap}>
            <PageTitle value="시스템 로그" />

            <section className={styles.filters}>
                <div className={styles.filterTop}>
                    <div className={styles.searchWrap}>
                        <Search
                            value={searchInput}
                            width={160}
                            placeholder="traceId 검색"
                            onChange={(value) => {
                                setSearchInput(value)
                                if (value.trim()) {
                                    setTraceIdMode("ALL")
                                }
                            }}
                            onSearch={(value) => {
                                const trimmedTraceId = value.trim()
                                setTraceId(trimmedTraceId)
                                if (trimmedTraceId) {
                                    setTraceIdMode("ALL")
                                }
                            }}
                        />
                    </div>
                    <Dropdown
                        options={traceIdModeOptions}
                        width={100}
                        value={traceIdMode}
                        onChange={(value) => {
                            setTraceIdMode(value)
                            setTraceId("")
                            setSearchInput("")
                        }}
                    />
                    <Dropdown
                        options={logLevelOptions}
                        width={120}
                        value={logLevel}
                        onChange={(value) => setLogLevel(value)}
                    />
                    <Dropdown
                        options={logTypeOptions}
                        width={100}
                        value={logType}
                        onChange={(value) => setLogType(value)}
                    />
                    <Dropdown
                        options={logPeriodOptions}
                        width={108}
                        value={logPeriod}
                        onChange={(value) => {
                            const option = value as LogPeriodOptionValue
                            setLogPeriod(option)

                            if (option === "custom") {
                                setCustomStart(toInputValueFromApiTimestamp(fromTimestamp))
                                setCustomEnd(toInputValueFromApiTimestamp(toTimestamp))
                                return
                            }

                            applyPresetRange(option)
                        }}
                    />
                    {logPeriod === "custom" && (
                        <div className={styles.customRangeWrap}>
                            <DatePicker
                                value={customStart}
                                max={formatForDatetimeInput(new Date())}
                                min={formatForDatetimeInput(
                                    getMaxLookbackStart(new Date())
                                )}
                                onChange={setCustomStart}
                            />
                            <span className={styles.rangeDivider}>~</span>
                            <DatePicker
                                value={customEnd}
                                max={formatForDatetimeInput(new Date())}
                                min={formatForDatetimeInput(
                                    getMaxLookbackStart(new Date())
                                )}
                                onChange={setCustomEnd}
                            />
                        </div>
                    )}
                </div>

                <div className={styles.filterBottom}>
                    <span className={styles.lastFetchedAt}>
                        마지막 조회 시간 {formatLastFetchedAt(dataUpdatedAt)}
                    </span>
                    <div className={styles.filterBottomRight}>
                        <div className={styles.right}>
                            <Dropdown
                                options={sizeOptions}
                                width={72}
                                value={size}
                                onChange={(value) => setSize(value)}
                            />
                            <Dropdown
                                options={pollingOptions}
                                width={78}
                                value={pollingMs}
                                onChange={(value) => setPollingMs(value)}
                            />
                        </div>
                        <Button
                            type="button"
                            variant="outline"
                            disabled={isRefreshLoading}
                            onClick={async () => {
                                if (logPeriod !== "custom") {
                                    applyPresetRange(logPeriod)
                                    scrollToBottom()
                                    return
                                }
                                await refetch()
                                scrollToBottom()
                            }}
                        >
                            {isRefreshLoading ? "loading" : "refresh"}
                        </Button>
                    </div>
                </div>
            </section>

            <section className={styles.viewerWrap}>
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
