import { http, type ApiRequestConfig } from "./client"

export type BookmarkToggleCommand = {
  postId: number
}

export type BookmarkToggleResult = {
  bookmarked: boolean
}

export const bookmarkApi = {
  toggle: async (
    command: BookmarkToggleCommand,
    config?: ApiRequestConfig
  ): Promise<BookmarkToggleResult> => {
    const res = await http.post<BookmarkToggleResult>("/api/bookmark", command, config)
    return res ?? { bookmarked: false }
  },
}
