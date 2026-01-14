import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { postsApi } from "../api/post.api.js"

export function usePostsQuery({ page } = {}) {
    return useQuery({
        queryKey: ["posts", { page: page ?? 1 }],
        queryFn: ({ signal }) => postsApi.list({ page }, { signal }),
        keepPreviousData: true,
    })
}

export function usePostsByTechBlogKeyQuery({ page, techBlogKey }) {
    return useQuery({
        queryKey: ["posts", "techBlog", { techBlogKey, page: page ?? 1 }],
        queryFn: ({ signal }) => postsApi.listByTechBlogKey({ page, techBlogKey }, { signal }),
        enabled: !!techBlogKey,
        keepPreviousData: true,
    })
}

export function usePostsBySubscriptionQuery({ page } = {}) {
    return useQuery({
        queryKey: ["posts", "subscribed", { page: page ?? 1 }],
        queryFn: ({ signal }) => postsApi.listBySubscription({ page }, { signal }),
        keepPreviousData: true,
    })
}

export function usePostsByBookmarkQuery({ page } = {}) {
    return useQuery({
        queryKey: ["posts", "bookmarked", { page: page ?? 1 }],
        queryFn: ({ signal }) => postsApi.listByBookmark({ page }, { signal }),
        keepPreviousData: true,
    })
}

export function useIncreasePostViewCountMutation() {
    return useMutation({
        mutationFn: postsApi.increaseViewCount,
    })
}