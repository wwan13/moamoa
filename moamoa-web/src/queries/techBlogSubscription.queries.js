import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { techBlogSubscriptionApi } from "../api/techBlogSubscription.api.js"

export function useSubscribingBlogsQuery() {
    return useQuery({
        queryKey: ["techBlogs", "subscribed"],
        queryFn: ({ signal }) => techBlogSubscriptionApi.list({ signal }),
        staleTime: 10_000,
    })
}

export function useSubscriptionToggleMutation() {
    const qc = useQueryClient()

    return useMutation({
        mutationFn: techBlogSubscriptionApi.toggleSubscription,
        onSuccess: () => {
            // 구독 목록/구독 기반 포스트/카테고리 UI 등에 영향
            qc.invalidateQueries({ queryKey: ["techBlogs", "subscribed"] })
            qc.invalidateQueries({ queryKey: ["posts", "subscribed"] })
            qc.invalidateQueries({ queryKey: ["categories"] })
        },
    })
}

export function useNotificationToggleMutation() {
    const qc = useQueryClient()

    return useMutation({
        mutationFn: techBlogSubscriptionApi.toggleNotification,
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ["techBlogs", "subscribed"] })
        },
    })
}