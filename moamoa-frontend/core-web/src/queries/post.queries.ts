import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from "@tanstack/react-query"
import {
  postsApi,
  type PostList,
  type PostListConditions,
  type PostPagingConditions,
  type TechBlogPostConditions,
  type ViewedPostResult,
  type ViewPostCommand,
} from "../api/post.api"
import useAuth from "../auth/useAuth"

type QueryOptions = {
  enabled?: boolean
}

const READ_POST_IDS_QUERY_KEY = ["posts", "read"] as const
const READ_POST_IDS_CACHE_NAME = "moamoa-post-state"
const READ_POST_IDS_CACHE_KEY = "/__cache__/read-post-ids"
const MAX_READ_POST_IDS = 500

const loadReadPostIds = async (): Promise<number[]> => {
  if (!("caches" in globalThis)) return []

  try {
    const cache = await globalThis.caches.open(READ_POST_IDS_CACHE_NAME)
    const response = await cache.match(READ_POST_IDS_CACHE_KEY)
    if (!response) return []

    const parsed = await response.json()
    if (!Array.isArray(parsed)) return []

    return parsed.filter((value): value is number => Number.isInteger(value))
  } catch {
    return []
  }
}

const saveReadPostIds = async (postIds: number[]): Promise<void> => {
  if (!("caches" in globalThis)) return

  try {
    const cache = await globalThis.caches.open(READ_POST_IDS_CACHE_NAME)
    await cache.put(
      READ_POST_IDS_CACHE_KEY,
      new Response(JSON.stringify(postIds.slice(-MAX_READ_POST_IDS)), {
        headers: {
          "Content-Type": "application/json",
          "Cache-Control": "no-store",
        },
      }),
    )
  } catch {
    // Ignore cache write failures and keep in-memory cache working.
  }
}

export const useReadPostIdsQuery = () => {
  return useQuery<number[]>({
    queryKey: READ_POST_IDS_QUERY_KEY,
    queryFn: loadReadPostIds,
    staleTime: Number.POSITIVE_INFINITY,
    gcTime: Number.POSITIVE_INFINITY,
  })
}

export const markPostAsRead = (
  queryClient: QueryClient,
  postId: number,
): void => {
  queryClient.setQueryData<number[]>(READ_POST_IDS_QUERY_KEY, (current = []) => {
    if (current.includes(postId)) return current

    const next = [...current, postId]
    void saveReadPostIds(next)
    return next
  })
}

export const usePostsQuery = (
  conditions: PostListConditions = {},
  options: QueryOptions = {},
) => {
  const { authScope, publicScope } = useAuth()
  const scope = authScope ?? publicScope

  const resolvedPage = conditions.page ?? 1
  const resolvedSize = conditions.size ?? 20
  const hasQuery = !!conditions.query

  return useQuery<PostList>({
    queryKey: [
      "posts",
      scope,
      "list",
      {
        page: resolvedPage,
        size: resolvedSize,
        query: hasQuery ? conditions.query : undefined,
        category: conditions.category,
      },
    ],
    queryFn: ({ signal }) =>
      postsApi.list(
        {
          page: resolvedPage,
          size: resolvedSize,
          query: conditions.query,
          category: conditions.category,
        },
        { signal },
      ),
    enabled: options.enabled ?? true,
    staleTime: hasQuery ? 0 : 1000 * 30,
    gcTime: hasQuery ? 0 : 1000 * 60 * 5,
  })
}

export const usePostsByTechBlogIdQuery = (
  conditions: TechBlogPostConditions = {},
  options: QueryOptions = {},
) => {
  const { authScope, publicScope } = useAuth()
  const scope = authScope ?? publicScope

  return useQuery<PostList>({
    queryKey: [
      "posts",
      scope,
      "techBlog",
      {
        techBlogId: conditions.techBlogId,
        page: conditions.page ?? 1,
        category: conditions.category,
      },
    ],
    queryFn: ({ signal }) => postsApi.listByTechBlogId(conditions, { signal }),
    enabled: (options.enabled ?? true) && !!conditions.techBlogId,
  })
}

export const usePostsBySubscriptionQuery = (
  conditions: PostPagingConditions = {},
  options: QueryOptions = {},
) => {
  const { authScope } = useAuth()

  return useQuery<PostList>({
    queryKey: [
      "posts",
      authScope,
      "subscribed",
      { page: conditions.page ?? 1, category: conditions.category },
    ],
    queryFn: ({ signal }) =>
      postsApi.listBySubscription(conditions, { signal }),
    enabled: (options.enabled ?? true) && !!authScope,
  })
}

export const usePostsByBookmarkQuery = (
  conditions: PostPagingConditions = {},
  options: QueryOptions = {},
) => {
  const { authScope } = useAuth()

  return useQuery<PostList>({
    queryKey: [
      "posts",
      authScope,
      "bookmarked",
      { page: conditions.page ?? 1, category: conditions.category },
    ],
    queryFn: ({ signal }) => postsApi.listByBookmark(conditions, { signal }),
    enabled: (options.enabled ?? true) && !!authScope,
  })
}

export const useInfinitePostsQuery = (
  conditions: Pick<PostListConditions, "size" | "query"> = {},
  options: QueryOptions = {},
) => {
  const { authScope, publicScope } = useAuth()
  const scope = authScope ?? publicScope

  const resolvedSize = conditions.size ?? 10
  const hasQuery = !!conditions.query

  return useInfiniteQuery<PostList>({
    queryKey: [
      "posts",
      scope,
      "infinite",
      {
        size: resolvedSize,
        query: hasQuery ? conditions.query : undefined,
      },
    ],
    queryFn: ({ pageParam = 1, signal }) =>
      postsApi.list(
        {
          page: Number(pageParam),
          size: resolvedSize,
          query: conditions.query,
        },
        { signal },
      ),
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

export const useViewPostMutation = () => {
  const queryClient = useQueryClient()

  return useMutation<ViewedPostResult | null, Error, ViewPostCommand>({
    mutationFn: (command) => postsApi.viewPost(command),
    onSuccess: (_data, variables) => {
      markPostAsRead(queryClient, Number(variables.postId))
    },
  })
}
