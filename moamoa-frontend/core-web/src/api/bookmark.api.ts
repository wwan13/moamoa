import { http, type ApiRequestConfig } from "./client"

export type BookmarkCommand = {
  postId: number
}

export const bookmarkApi = {
  bookmark: async (
    command: BookmarkCommand,
    config?: ApiRequestConfig,
  ): Promise<void> => {
    await http.post<void>("/api/bookmark", command, config)
  },
  unbookmark: async (
    command: BookmarkCommand,
    config?: ApiRequestConfig,
  ): Promise<void> => {
    await http.del<void>("/api/bookmark", command, config)
  },
}
