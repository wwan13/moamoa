import { useQuery } from "@tanstack/react-query"
import {
    logApi,
    type AdminLogPage,
    type AdminLogQueryConditions,
} from "../api/log.api"

export const useLogsQuery = (
    conditions: AdminLogQueryConditions,
    pollingMs: number
) => {
    return useQuery<AdminLogPage>({
        queryKey: ["admin-logs", conditions],
        queryFn: () => logApi.findByConditions(conditions),
        refetchInterval: pollingMs > 0 ? pollingMs : false,
    })
}
