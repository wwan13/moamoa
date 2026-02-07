import { useQuery, useMutation, useInfiniteQuery } from "@tanstack/react-query"
import { postsApi } from "../api/post.api.js"
import useAuth from "../auth/AuthContext.jsx"

export function usePostsQuery(
    { page, size = 20, query } = {},
    options = {}
) {
    const { authScope, publicScope } = useAuth()
    const scope = authScope ?? publicScope

    const resolvedPage = page ?? 1
    const resolvedSize = size ?? SIZE
    const hasQuery = !!query

    return useQuery({
        queryKey: ["posts", scope, "list", {
            page: resolvedPage,
            size: resolvedSize,
            query: hasQuery ? query : undefined,
        }],
        queryFn: ({ signal }) =>
            postsApi.list(
                { page: resolvedPage, size: resolvedSize, query },
                { signal }
            ),
        enabled: options.enabled ?? true,

        staleTime: hasQuery ? 0 : 1000 * 30,
        gcTime: hasQuery ? 0 : 1000 * 60 * 5,

        keepPreviousData: !hasQuery,
    })
}

export function usePostsByTechBlogIdQuery({ page, techBlogId } = {}, options = {}) {
    const { authScope, publicScope } = useAuth()
    const scope = authScope ?? publicScope

    return useQuery({
        queryKey: ["posts", scope, "techBlog", { techBlogId, page: page ?? 1 }],
        queryFn: ({ signal }) =>
            postsApi.listByTechBlogId({ page, techBlogId }, { signal }),
        enabled: (options.enabled ?? true) && !!techBlogId,
        keepPreviousData: true,
    })
}

export function usePostsBySubscriptionQuery({ page } = {}, options = {}) {
    const { authScope } = useAuth()

    return useQuery({
        queryKey: ["posts", authScope, "subscribed", { page: page ?? 1 }],
        queryFn: ({ signal }) => postsApi.listBySubscription({ page }, { signal }),
        enabled: (options.enabled ?? true) && !!authScope,
        keepPreviousData: true,
    })
}

export function usePostsByBookmarkQuery({ page } = {}, options = {}) {
    const { authScope } = useAuth()

    return useQuery({
        queryKey: ["posts", authScope, "bookmarked", { page: page ?? 1 }],
        queryFn: ({ signal }) => postsApi.listByBookmark({ page }, { signal }),
        enabled: (options.enabled ?? true) && !!authScope,
        keepPreviousData: true,
    })
}

export function useInfinitePostsQuery(
    { size = 10, query } = {},
    options = {}
) {
    const { authScope, publicScope } = useAuth()
    const scope = authScope ?? publicScope

    const resolvedSize = size ?? 10
    const hasQuery = !!query

    return useInfiniteQuery({
        queryKey: ["posts", scope, "infinite", {
            size: resolvedSize,
            query: hasQuery ? query : undefined,
        }],
        queryFn: ({ pageParam = 1, signal }) =>
            postsApi.list(
                { page: pageParam, size: resolvedSize, query },
                { signal }
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

export function useIncreasePostViewCountMutation() {
    return useMutation({
        mutationFn: postsApi.increaseViewCount,
    })
}
