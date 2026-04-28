import { useMutation, useQueryClient, type QueryClient } from "@tanstack/react-query"
import {
  subscriptionApi,
  type SubscriptionCommand,
} from "../api/subscriptionApi.ts"
import useAuth from "../auth/useAuth"
import type { TechBlogList, TechBlogSummary } from "../api/techBlog.api"

type MutationOptions = {
  invalidateOnSuccess?: boolean
}

type PatchSubscribedTechBlogsCacheParams = {
  queryClient: QueryClient
  authScope: string | null
  techBlog: TechBlogSummary
  subscribed: boolean
  notificationEnabled?: boolean
}

export const patchSubscribedTechBlogsCache = ({
  queryClient,
  authScope,
  techBlog,
  subscribed,
  notificationEnabled = techBlog.notificationEnabled,
}: PatchSubscribedTechBlogsCacheParams) => {
  if (!authScope) return

  queryClient.setQueryData<TechBlogList | undefined>(
    ["techBlogs", "subscribed", authScope],
    (old) => {
      const nextBlog: TechBlogSummary = {
        ...techBlog,
        subscribed,
        notificationEnabled,
      }

      if (!old) {
        if (!subscribed) return old
        return {
          meta: { totalCount: 1 },
          techBlogs: [nextBlog],
        }
      }

      const exists = old.techBlogs.some((blog) => blog.id === techBlog.id)
      const techBlogs = subscribed
        ? exists
          ? old.techBlogs.map((blog) => (blog.id === techBlog.id ? nextBlog : blog))
          : [nextBlog, ...old.techBlogs]
        : old.techBlogs.filter((blog) => blog.id !== techBlog.id)

      return {
        ...old,
        meta: {
          ...old.meta,
          totalCount: techBlogs.length,
        },
        techBlogs,
      }
    },
  )
}

const useInvalidateSubscriptionQueries = (options: MutationOptions = {}) => {
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

  return useMutation<void, Error, SubscriptionCommand>({
    mutationFn: (command) => subscriptionApi.subscribe(command),
    onSuccess: invalidate,
  })
}

export const useUnsubscribeMutation = (options: MutationOptions = {}) => {
  const { invalidate } = useInvalidateSubscriptionQueries(options)

  return useMutation<void, Error, SubscriptionCommand>({
    mutationFn: (command) => subscriptionApi.unsubscribe(command),
    onSuccess: invalidate,
  })
}

export const useEnableNotificationMutation = () => {
  return useMutation<void, Error, SubscriptionCommand>({
    mutationFn: (command) => subscriptionApi.enableNotification(command),
  })
}

export const useDisableNotificationMutation = () => {
  return useMutation<void, Error, SubscriptionCommand>({
    mutationFn: (command) => subscriptionApi.disableNotification(command),
  })
}
