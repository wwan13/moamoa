import { type ApiRequestConfig, http } from "./client"

export type SubscriptionCommand = {
  techBlogId: number
}

export type SubscriptionResult = {
  subscribing: boolean
}

export type NotificationEnabledResult = {
  notificationEnabled: boolean
}

export const subscriptionApi = {
  subscribe: async (
    command: SubscriptionCommand,
    config?: ApiRequestConfig,
  ): Promise<SubscriptionResult> => {
    const res = await http.post<SubscriptionResult>(
      "/api/subscription",
      command,
      config,
    )

    return res ?? { subscribing: false }
  },
  unsubscribe: async (
    command: SubscriptionCommand,
    config?: ApiRequestConfig,
  ): Promise<SubscriptionResult> => {
    const res = await http.del<SubscriptionResult>("/api/subscription", command, config)

    return res ?? { subscribing: false }
  },
  enableNotification: async (
    command: SubscriptionCommand,
    config?: ApiRequestConfig,
  ): Promise<NotificationEnabledResult> => {
    const res = await http.post<NotificationEnabledResult>(
      "/api/subscription/notification-enabled",
      command,
      config,
    )

    return res ?? { notificationEnabled: false }
  },
  disableNotification: async (
    command: SubscriptionCommand,
    config?: ApiRequestConfig,
  ): Promise<NotificationEnabledResult> => {
    const res = await http.del<NotificationEnabledResult>(
      "/api/subscription/notification-enabled",
      command,
      config,
    )

    return res ?? { notificationEnabled: false }
  },
}
