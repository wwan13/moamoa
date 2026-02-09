import { useQuery } from "@tanstack/react-query"
import {
  techBlogApi,
  type TechBlogList,
  type TechBlogSummary,
} from "../api/techBlog.api"
import useAuth from "../auth/useAuth"

type TechBlogListQuery = {
  query?: string
}

type QueryOptions = {
  enabled?: boolean
}

export const useTechBlogsQuery = (
  conditions: TechBlogListQuery = {},
  options: QueryOptions = {}
) => {
  const { authScope, publicScope } = useAuth()
  const scope = authScope ?? publicScope

  const hasQuery = !!conditions.query

  return useQuery<TechBlogList>({
    queryKey: ["techBlogs", scope, { query: hasQuery ? conditions.query : undefined }],
    queryFn: ({ signal }) => techBlogApi.list({ query: conditions.query }, { signal }),
    enabled: options.enabled ?? true,
    staleTime: hasQuery ? 0 : 60 * 1000,
    gcTime: hasQuery ? 0 : 5 * 60 * 1000,
  })
}

export const useTechBlogByIdQuery = ({ techBlogId }: { techBlogId?: string | number }) => {
  const { authScope, publicScope } = useAuth()
  const scope = authScope ?? publicScope

  return useQuery<TechBlogSummary | null>({
    queryKey: ["techBlog", scope, String(techBlogId)],
    queryFn: ({ signal }) => techBlogApi.findById({ techBlogId: techBlogId ?? "" }, { signal }),
    enabled: !!techBlogId,
  })
}

export const useSubscribingTechBlogsQuery = () => {
  const { authScope } = useAuth()

  return useQuery<TechBlogList>({
    queryKey: ["techBlogs", "subscribed", authScope],
    queryFn: ({ signal }) => techBlogApi.listSubscribed({ signal }),
    enabled: !!authScope,
    staleTime: 10 * 1000,
  })
}
