import { useQuery, useMutation } from "@tanstack/react-query"
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

export function usePostsByTechBlogKeyQuery({ page, techBlogKey } = {}, options = {}) {
    const { authScope, publicScope } = useAuth()
    const scope = authScope ?? publicScope

    return useQuery({
        queryKey: ["posts", scope, "techBlog", { techBlogKey, page: page ?? 1 }],
        queryFn: ({ signal }) =>
            postsApi.listByTechBlogKey({ page, techBlogKey }, { signal }),
        enabled: (options.enabled ?? true) && !!techBlogKey,
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

export function useIncreasePostViewCountMutation() {
    return useMutation({
        mutationFn: postsApi.increaseViewCount,
    })
}