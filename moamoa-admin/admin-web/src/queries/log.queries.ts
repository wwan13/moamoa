import { useInfiniteQuery } from "@tanstack/react-query"
import {
    logApi,
    type AdminLogPage,
    type AdminLogQueryConditions,
} from "../api/log.api"

type LogCursorPageParam = {
    cursorTimestamp?: string
    cursorId?: number
}

export const useInfiniteLogsQuery = (
    conditions: AdminLogQueryConditions,
    pollingMs: number
) => {
    return useInfiniteQuery<AdminLogPage, Error, AdminLogPage, [string, AdminLogQueryConditions], LogCursorPageParam>({
        queryKey: ["admin-logs", conditions],
        initialPageParam: {},
        queryFn: ({ pageParam }) =>
            logApi.findByConditions({
                ...conditions,
                cursorTimestamp: pageParam.cursorTimestamp,
                cursorId: pageParam.cursorId,
            }),
        getNextPageParam: (lastPage) => {
            if (!lastPage.hasNext || !lastPage.nextCursor) return undefined
            return {
                cursorTimestamp: lastPage.nextCursor.timestamp,
                cursorId: lastPage.nextCursor.id,
            }
        },
        refetchInterval: pollingMs > 0 ? pollingMs : false,
    })
}
