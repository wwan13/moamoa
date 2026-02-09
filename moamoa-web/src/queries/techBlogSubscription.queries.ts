import { useMutation, useQueryClient } from "@tanstack/react-query"
import {
  techBlogSubscriptionApi,
  type NotificationEnabledToggleCommand,
  type NotificationEnabledToggleResult,
  type TechBlogSubscriptionToggleCommand,
  type TechBlogSubscriptionToggleResult,
} from "../api/techBlogSubscription.api"
import useAuth from "../auth/useAuth"

type ToggleOptions = {
  invalidateOnSuccess?: boolean
}

export const useSubscriptionToggleMutation = (options: ToggleOptions = {}) => {
  const qc = useQueryClient()
  const { authScope } = useAuth()

  const invalidateOnSuccess = options.invalidateOnSuccess ?? true

  return useMutation<TechBlogSubscriptionToggleResult, Error, TechBlogSubscriptionToggleCommand>({
    mutationFn: (command) => techBlogSubscriptionApi.toggleSubscription(command),
    onSuccess: () => {
      if (!invalidateOnSuccess) return
      qc.invalidateQueries({ queryKey: ["techBlogs", "subscribed", authScope] })
      qc.invalidateQueries({ queryKey: ["posts", authScope, "subscribed"] })
      qc.invalidateQueries({ queryKey: ["techBlogs", authScope] })
      qc.invalidateQueries({ queryKey: ["member", authScope] })
    },
  })
}

export const useNotificationToggleMutation = () => {
  return useMutation<NotificationEnabledToggleResult, Error, NotificationEnabledToggleCommand>({
    mutationFn: (command) => techBlogSubscriptionApi.toggleNotification(command),
  })
}
