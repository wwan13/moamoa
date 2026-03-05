import {type ApiRequestConfig, http} from "./client"

export type SubscriptionToggleCommand = {
  techBlogId: number
}

export type SubscriptionToggleResult = {
  subscribing: boolean
}

export type NotificationEnabledToggleCommand = {
  techBlogId: number
}

export type NotificationEnabledToggleResult = {
  notificationEnabled: boolean
}

export const subscriptionApi = {
  toggleSubscription: async (
    command: SubscriptionToggleCommand,
    config?: ApiRequestConfig
  ): Promise<SubscriptionToggleResult> => {
    const res = await http.post<SubscriptionToggleResult>(
      "/api/subscription",
      command,
      config
    )

    return res ?? { subscribing: false }
  },
  toggleNotification: async (
    command: NotificationEnabledToggleCommand,
    config?: ApiRequestConfig
  ): Promise<NotificationEnabledToggleResult> => {
    const res = await http.patch<NotificationEnabledToggleResult>(
      "/api/subscription/notification-enabled",
      command,
      config
    )

    return res ?? { notificationEnabled: false }
  },
}
