import { useMutation, useQueryClient } from "@tanstack/react-query"
import {
  subscriptionApi,
  type NotificationEnabledResult,
  type SubscriptionCommand,
  type SubscriptionResult,
} from "../api/subscriptionApi.ts"
import useAuth from "../auth/useAuth"

type MutationOptions = {
  invalidateOnSuccess?: boolean
}

const useInvalidateSubscriptionQueries = (
  options: MutationOptions = {},
) => {
  const qc = useQueryClient()
  const { authScope } = useAuth()
  const invalidateOnSuccess = options.invalidateOnSuccess ?? true

  const invalidate = () => {
    if (!invalidateOnSuccess) return
    qc.invalidateQueries({ queryKey: ["techBlogs", "subscribed", authScope] })
    qc.invalidateQueries({ queryKey: ["posts", authScope, "subscribed"] })
    qc.invalidateQueries({ queryKey: ["techBlogs", authScope] })
    qc.invalidateQueries({ queryKey: ["member", authScope] })
  }

  return { invalidate }
}

export const useSubscribeMutation = (options: MutationOptions = {}) => {
  const { invalidate } = useInvalidateSubscriptionQueries(options)

  return useMutation<SubscriptionResult, Error, SubscriptionCommand>({
    mutationFn: (command) => subscriptionApi.subscribe(command),
    onSuccess: invalidate,
  })
}

export const useUnsubscribeMutation = (options: MutationOptions = {}) => {
  const { invalidate } = useInvalidateSubscriptionQueries(options)

  return useMutation<SubscriptionResult, Error, SubscriptionCommand>({
    mutationFn: (command) => subscriptionApi.unsubscribe(command),
    onSuccess: invalidate,
  })
}

export const useEnableNotificationMutation = () => {
  return useMutation<NotificationEnabledResult, Error, SubscriptionCommand>({
    mutationFn: (command) => subscriptionApi.enableNotification(command),
  })
}

export const useDisableNotificationMutation = () => {
  return useMutation<NotificationEnabledResult, Error, SubscriptionCommand>({
    mutationFn: (command) => subscriptionApi.disableNotification(command),
  })
}
