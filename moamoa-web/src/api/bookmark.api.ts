import { http, type ApiRequestConfig } from "./client"

export type BookmarkCommand = {
  postId: number
}

export type BookmarkResult = {
  bookmarked: boolean
}

export const bookmarkApi = {
  bookmark: async (
    command: BookmarkCommand,
    config?: ApiRequestConfig,
  ): Promise<BookmarkResult> => {
    const res = await http.post<BookmarkResult>("/api/bookmark", command, config)
    return res ?? { bookmarked: false }
  },
  unbookmark: async (
    command: BookmarkCommand,
    config?: ApiRequestConfig,
  ): Promise<BookmarkResult> => {
    const res = await http.del<BookmarkResult>("/api/bookmark", command, config)
    return res ?? { bookmarked: false }
  },
}
