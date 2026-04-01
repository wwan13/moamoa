import { useQuery } from "@tanstack/react-query"
import useAuth from "../auth/useAuth"
import { noticeApi, type NoticeList } from "../api/notice.api"

type NoticeQueryOptions = {
  page?: number
  size?: number
  query?: string
}

export const useNoticesQuery = (conditions: NoticeQueryOptions = {}) => {
  const { authScope, publicScope } = useAuth()
  const scope = authScope ?? publicScope

  const page = conditions.page ?? 1
  const size = conditions.size ?? 10
  const query = conditions.query ?? ""

  return useQuery<NoticeList>({
    queryKey: [
      "notice",
      scope,
      "list",
      { page, size, query: query || undefined },
    ],
    queryFn: ({ signal }) => noticeApi.list({ page, size, query }, { signal }),
    staleTime: 60 * 1000,
  })
}
