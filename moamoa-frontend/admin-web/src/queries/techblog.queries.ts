import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  type AdminDeleteTechBlogPostsResult,
  techBlogApi,
  type AdminCollectPostsResult,
  type AdminTechBlogSummary,
} from "../api/techblog.api"

export function useTechBlogsQuery() {
  return useQuery<AdminTechBlogSummary[]>({
    queryKey: ["admin-tech-blogs"],
    queryFn: () => techBlogApi.findAll(),
  })
}

export function useCollectTechBlogPostsMutation() {
  const queryClient = useQueryClient()

  return useMutation<AdminCollectPostsResult, Error, { techBlogId: number }>({
    mutationFn: ({ techBlogId }) => techBlogApi.collectPosts({ techBlogId }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-tech-blogs"] })
    },
  })
}

export function useDeleteTechBlogPostsMutation() {
  const queryClient = useQueryClient()

  return useMutation<AdminDeleteTechBlogPostsResult, Error, { techBlogId: number }>({
    mutationFn: ({ techBlogId }) => techBlogApi.deletePosts(techBlogId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-tech-blogs"] })
    },
  })
}
