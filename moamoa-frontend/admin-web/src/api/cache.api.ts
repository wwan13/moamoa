import { http } from "./client"

export type AdminCacheSummary = {
  key: string
  name: string
  description: string
  target: string
  evictionStrategy: "exact_key" | "prefix" | "versioned_prefix" | "unsupported"
  evictable: boolean
  unsupportedReason?: string | null
}

export type AdminCacheEvictResult = {
  key: string
  name: string
  evictionStrategy: "exact_key" | "prefix" | "versioned_prefix" | "unsupported"
}

export type AdminCacheEvictAllResult = {
  evicted: AdminCacheEvictResult[]
  skipped: AdminCacheSummary[]
}

export const cacheApi = {
  findAll: async (): Promise<AdminCacheSummary[]> => {
    const res = await http.get<AdminCacheSummary[]>("/api/admin/cache")
    if (res) return res

    return []
  },
  evict: async (cacheKey: string): Promise<AdminCacheEvictResult> => {
    const res = await http.post<AdminCacheEvictResult>(
      `/api/admin/cache/${cacheKey}/evict`,
      {},
    )

    if (res) return res

    throw new Error("EMPTY_CACHE_EVICT_RESPONSE")
  },
  evictAll: async (): Promise<AdminCacheEvictAllResult> => {
    const res = await http.post<AdminCacheEvictAllResult>(
      "/api/admin/cache/evict-all",
      {},
    )

    if (res) return res

    throw new Error("EMPTY_CACHE_EVICT_ALL_RESPONSE")
  },
}
