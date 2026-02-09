import { http, type ApiRequestConfig } from "./client"

export type CategoryData = {
  id: number
  title: string
  key: string
}

export const categoryApi = {
  list: async (config?: ApiRequestConfig): Promise<CategoryData[]> => {
    const res = await http.get<CategoryData[]>("/api/category", config)
    return res ?? []
  },
}
