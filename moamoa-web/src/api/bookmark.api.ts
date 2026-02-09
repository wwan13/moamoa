import { http, type ApiRequestConfig } from "./client"

export type PostBookmarkToggleCommand = {
  postId: number
}

export type PostBookmarkToggleResult = {
  bookmarked: boolean
}

export const bookmarkApi = {
  toggle: async (
    command: PostBookmarkToggleCommand,
    config?: ApiRequestConfig
  ): Promise<PostBookmarkToggleResult> => {
    const res = await http.post<PostBookmarkToggleResult>("/api/post-bookmark", command, config)
    return res ?? { bookmarked: false }
  },
}
