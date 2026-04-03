import { type ApiRequestConfig, http } from "./client"

export type SubscriptionCommand = {
  techBlogId: number
}

export const subscriptionApi = {
  subscribe: async (
    command: SubscriptionCommand,
    config?: ApiRequestConfig,
  ): Promise<void> => {
    await http.post<void>("/api/subscription", command, config)
  },
  unsubscribe: async (
    command: SubscriptionCommand,
    config?: ApiRequestConfig,
  ): Promise<void> => {
    await http.del<void>("/api/subscription", command, config)
  },
  enableNotification: async (
    command: SubscriptionCommand,
    config?: ApiRequestConfig,
  ): Promise<void> => {
    await http.post<void>(
      "/api/subscription/notification-enabled",
      command,
      config,
    )
  },
  disableNotification: async (
    command: SubscriptionCommand,
    config?: ApiRequestConfig,
  ): Promise<void> => {
    await http.del<void>(
      "/api/subscription/notification-enabled",
      command,
      config,
    )
  },
}
