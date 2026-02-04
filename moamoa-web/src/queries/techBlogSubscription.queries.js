import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { techBlogSubscriptionApi } from "../api/techBlogSubscription.api.js"
import useAuth from "../auth/AuthContext.jsx"

export function useSubscriptionToggleMutation(options = {}) {
    const qc = useQueryClient()
    const { authScope } = useAuth()

    const invalidateOnSuccess = options.invalidateOnSuccess ?? true

    return useMutation({
        mutationFn: techBlogSubscriptionApi.toggleSubscription,
        onSuccess: () => {
            if (!invalidateOnSuccess) return
            qc.invalidateQueries({ queryKey: ["techBlogs", "subscribed", authScope] })
            qc.invalidateQueries({ queryKey: ["posts", authScope, "subscribed"] })
            qc.invalidateQueries({ queryKey: ["techBlogs", authScope] })
            qc.invalidateQueries({ queryKey: ["member", authScope] })
        },
    })
}

export function useNotificationToggleMutation(options = {}) {
    return useMutation({
        mutationFn: techBlogSubscriptionApi.toggleNotification,
    })
}