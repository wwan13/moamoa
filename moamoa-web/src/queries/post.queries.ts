import { useInfiniteQuery, useMutation, useQuery } from "@tanstack/react-query"
import {
  postsApi,
  type IncreasePostViewCountCommand,
  type IncreaseViewCountResult,
  type PostList,
  type PostListConditions,
  type PostPagingConditions,
  type TechBlogPostConditions,
} from "../api/post.api"
import useAuth from "../auth/useAuth"

type QueryOptions = {
  enabled?: boolean
}

export const usePostsQuery = (
  conditions: PostListConditions = {},
  options: QueryOptions = {}
) => {
  const { authScope, publicScope } = useAuth()
  const scope = authScope ?? publicScope

  const resolvedPage = conditions.page ?? 1
  const resolvedSize = conditions.size ?? 20
  const hasQuery = !!conditions.query

  return useQuery<PostList>({
    queryKey: ["posts", scope, "list", {
      page: resolvedPage,
      size: resolvedSize,
      query: hasQuery ? conditions.query : undefined,
    }],
    queryFn: ({ signal }) =>
      postsApi.list({ page: resolvedPage, size: resolvedSize, query: conditions.query }, { signal }),
    enabled: options.enabled ?? true,
    staleTime: hasQuery ? 0 : 1000 * 30,
    gcTime: hasQuery ? 0 : 1000 * 60 * 5,
  })
}

export const usePostsByTechBlogIdQuery = (
  conditions: TechBlogPostConditions = {},
  options: QueryOptions = {}
) => {
  const { authScope, publicScope } = useAuth()
  const scope = authScope ?? publicScope

  return useQuery<PostList>({
    queryKey: ["posts", scope, "techBlog", { techBlogId: conditions.techBlogId, page: conditions.page ?? 1 }],
    queryFn: ({ signal }) => postsApi.listByTechBlogId(conditions, { signal }),
    enabled: (options.enabled ?? true) && !!conditions.techBlogId,
  })
}

export const usePostsBySubscriptionQuery = (
  conditions: PostPagingConditions = {},
  options: QueryOptions = {}
) => {
  const { authScope } = useAuth()

  return useQuery<PostList>({
    queryKey: ["posts", authScope, "subscribed", { page: conditions.page ?? 1 }],
    queryFn: ({ signal }) => postsApi.listBySubscription(conditions, { signal }),
    enabled: (options.enabled ?? true) && !!authScope,
  })
}

export const usePostsByBookmarkQuery = (
  conditions: PostPagingConditions = {},
  options: QueryOptions = {}
) => {
  const { authScope } = useAuth()

  return useQuery<PostList>({
    queryKey: ["posts", authScope, "bookmarked", { page: conditions.page ?? 1 }],
    queryFn: ({ signal }) => postsApi.listByBookmark(conditions, { signal }),
    enabled: (options.enabled ?? true) && !!authScope,
  })
}

export const useInfinitePostsQuery = (
  conditions: Pick<PostListConditions, "size" | "query"> = {},
  options: QueryOptions = {}
) => {
  const { authScope, publicScope } = useAuth()
  const scope = authScope ?? publicScope

  const resolvedSize = conditions.size ?? 10
  const hasQuery = !!conditions.query

  return useInfiniteQuery<PostList>({
    queryKey: ["posts", scope, "infinite", {
      size: resolvedSize,
      query: hasQuery ? conditions.query : undefined,
    }],
    queryFn: ({ pageParam = 1, signal }) =>
      postsApi.list({ page: Number(pageParam), size: resolvedSize, query: conditions.query }, { signal }),
    initialPageParam: 1,
    getNextPageParam: (lastPage) => {
      const page = Number(lastPage?.meta?.page ?? 1)
      const totalPages = Number(lastPage?.meta?.totalPages ?? 0)
      if (page < totalPages) return page + 1
      return undefined
    },
    enabled: (options.enabled ?? true) && hasQuery,
    staleTime: 0,
    gcTime: 0,
  })
}

export const useIncreasePostViewCountMutation = () => {
  return useMutation<IncreaseViewCountResult, Error, IncreasePostViewCountCommand>({
    mutationFn: (command) => postsApi.increaseViewCount(command),
  })
}
