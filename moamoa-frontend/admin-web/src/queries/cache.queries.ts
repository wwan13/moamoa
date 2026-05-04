import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  cacheApi,
  type AdminCacheEvictAllResult,
  type AdminCacheEvictResult,
  type AdminCacheSummary,
} from "../api/cache.api"

const ADMIN_CACHES_QUERY_KEY = ["admin-caches"]

export function useAdminCachesQuery() {
  return useQuery<AdminCacheSummary[]>({
    queryKey: ADMIN_CACHES_QUERY_KEY,
    queryFn: () => cacheApi.findAll(),
  })
}

export function useEvictAdminCacheMutation() {
  const queryClient = useQueryClient()

  return useMutation<AdminCacheEvictResult, Error, { cacheKey: string }>({
    mutationFn: ({ cacheKey }) => cacheApi.evict(cacheKey),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ADMIN_CACHES_QUERY_KEY })
    },
  })
}

export function useEvictAllAdminCachesMutation() {
  const queryClient = useQueryClient()

  return useMutation<AdminCacheEvictAllResult, Error>({
    mutationFn: () => cacheApi.evictAll(),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ADMIN_CACHES_QUERY_KEY })
    },
  })
}
