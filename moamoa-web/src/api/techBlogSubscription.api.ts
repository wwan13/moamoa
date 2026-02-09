import { http, type ApiRequestConfig } from "./client"
import type { TechBlogData } from "./techBlog.api"

export type TechBlogSubscriptionToggleCommand = {
  techBlogId: number
}

export type TechBlogSubscriptionToggleResult = {
  subscribing: boolean
}

export type NotificationEnabledToggleCommand = {
  techBlogId: number
}

export type NotificationEnabledToggleResult = {
  notificationEnabled: boolean
}

export const techBlogSubscriptionApi = {
  list: async (config?: ApiRequestConfig): Promise<TechBlogData[]> => {
    const res = await http.get<TechBlogData[]>("/api/tech-blog-subscription", config)
    return res ?? []
  },
  toggleSubscription: async (
    command: TechBlogSubscriptionToggleCommand,
    config?: ApiRequestConfig
  ): Promise<TechBlogSubscriptionToggleResult> => {
    const res = await http.post<TechBlogSubscriptionToggleResult>(
      "/api/tech-blog-subscription",
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
      "/api/tech-blog-subscription/notification-enabled",
      command,
      config
    )

    return res ?? { notificationEnabled: false }
  },
}
