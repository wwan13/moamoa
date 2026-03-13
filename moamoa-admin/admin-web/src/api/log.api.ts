import { http } from "./client"

export type AdminLogQueryConditions = {
    logLevel?: string
    type?: string
    traceId?: string
    size?: number
    cursorTimestamp?: string
    cursorId?: number
}

export type AdminLogSummary = {
    id: number
    timestamp: string
    logLevel: string
    traceId: string
    loggerName: string
    message: string
    type: string
    data: string
}

type AdminLogCursor = {
    timestamp: string
    id: number
}

export type AdminLogPage = {
    items: AdminLogSummary[]
    nextCursor: AdminLogCursor | null
    size: number
    hasNext: boolean
}

const buildLogQueryString = (conditions: AdminLogQueryConditions): string => {
    const params = new URLSearchParams()

    if (conditions.logLevel) params.set("logLevel", conditions.logLevel)
    if (conditions.type) params.set("type", conditions.type)
    if (conditions.traceId) params.set("traceId", conditions.traceId)
    if (conditions.size !== undefined) params.set("size", String(conditions.size))
    if (conditions.cursorTimestamp) {
        params.set("cursorTimestamp", conditions.cursorTimestamp)
    }
    if (conditions.cursorId !== undefined) {
        params.set("cursorId", String(conditions.cursorId))
    }

    const queryString = params.toString()
    return queryString ? `?${queryString}` : ""
}

export const logApi = {
    findByConditions: async (
        conditions: AdminLogQueryConditions
    ): Promise<AdminLogPage> => {
        const queryString = buildLogQueryString(conditions)
        const res = await http.get<AdminLogPage>(`/api/admin/log${queryString}`)
        if (res) return res

        return {
            items: [],
            nextCursor: null,
            size: conditions.size ?? 100,
            hasNext: false,
        }
    },
}
