import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  noticeApi,
  type AdminNoticeList,
  type AdminNoticeQueryConditions,
} from "../api/notice.api"

export const useNoticesQuery = (conditions: AdminNoticeQueryConditions) => {
  return useQuery<AdminNoticeList>({
    queryKey: ["admin-notices", conditions],
    queryFn: () => noticeApi.findByConditions(conditions),
  })
}

type UpdateNoticePublishedVariables = {
  noticeId: number
  published: boolean
}

type CreateNoticeVariables = {
  title: string
  chip: string
  content: string
  published: boolean
}

export function useUpdateNoticePublishedMutation() {
  const queryClient = useQueryClient()

  return useMutation<void, Error, UpdateNoticePublishedVariables>({
    mutationFn: ({ noticeId, published }) =>
      noticeApi.updatePublished(noticeId, { published }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-notices"] })
    },
  })
}

export function useCreateNoticeMutation() {
  const queryClient = useQueryClient()

  return useMutation<void, Error, CreateNoticeVariables>({
    mutationFn: (command) => noticeApi.create(command),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-notices"] })
    },
  })
}
