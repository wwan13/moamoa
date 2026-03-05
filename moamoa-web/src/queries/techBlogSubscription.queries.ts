import { useMutation, useQueryClient } from "@tanstack/react-query"
import {
  subscriptionApi,
  type NotificationEnabledToggleCommand,
  type NotificationEnabledToggleResult,
  type SubscriptionToggleCommand,
  type SubscriptionToggleResult,
} from "../api/subscriptionApi.ts"
import useAuth from "../auth/useAuth"

type ToggleOptions = {
  invalidateOnSuccess?: boolean
}

export const useSubscriptionToggleMutation = (options: ToggleOptions = {}) => {
  const qc = useQueryClient()
  const { authScope } = useAuth()

  const invalidateOnSuccess = options.invalidateOnSuccess ?? true

  return useMutation<SubscriptionToggleResult, Error, SubscriptionToggleCommand>({
    mutationFn: (command) => subscriptionApi.toggleSubscription(command),
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
    mutationFn: (command) => subscriptionApi.toggleNotification(command),
  })
}
