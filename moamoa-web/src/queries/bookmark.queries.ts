import { useMutation, useQueryClient } from "@tanstack/react-query"
import {
  bookmarkApi,
  type PostBookmarkToggleCommand,
  type PostBookmarkToggleResult,
} from "../api/bookmark.api"
import useAuth from "../auth/useAuth"

type BookmarkToggleOptions = {
  invalidateOnSuccess?: boolean
}

export const useBookmarkToggleMutation = (options: BookmarkToggleOptions = {}) => {
  const qc = useQueryClient()
  const { authScope, publicScope } = useAuth()
  const scope = authScope ?? publicScope

  const invalidateOnSuccess = options.invalidateOnSuccess ?? true

  return useMutation<PostBookmarkToggleResult, Error, PostBookmarkToggleCommand>({
    mutationFn: (command) => bookmarkApi.toggle(command),
    onSuccess: (_data, variables) => {
      if (!invalidateOnSuccess) return

      qc.invalidateQueries({ queryKey: ["posts", scope] })
      qc.invalidateQueries({ queryKey: ["post", scope, variables.postId] })
      qc.invalidateQueries({ queryKey: ["member", authScope] })
    },
  })
}
