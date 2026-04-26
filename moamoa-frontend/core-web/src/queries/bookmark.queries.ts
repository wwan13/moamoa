import { useMutation, useQueryClient } from "@tanstack/react-query"
import { bookmarkApi, type BookmarkCommand } from "../api/bookmark.api"
import useAuth from "../auth/useAuth"

type BookmarkMutationOptions = {
  invalidateOnSuccess?: boolean
}

const useInvalidateBookmarkQueries = (
  options: BookmarkMutationOptions = {},
) => {
  const qc = useQueryClient()
  const { authScope, publicScope } = useAuth()
  const scope = authScope ?? publicScope
  const invalidateOnSuccess = options.invalidateOnSuccess ?? true

  const invalidate = (postId: number) => {
    if (!invalidateOnSuccess) return

    qc.invalidateQueries({ queryKey: ["posts", scope] })
    qc.invalidateQueries({ queryKey: ["post", scope, postId] })
    qc.invalidateQueries({ queryKey: ["member", authScope] })
  }

  return { invalidate }
}

export const useBookmarkMutation = (options: BookmarkMutationOptions = {}) => {
  const { invalidate } = useInvalidateBookmarkQueries(options)

  return useMutation<void, Error, BookmarkCommand>({
    mutationFn: (command) => bookmarkApi.bookmark(command),
    onSuccess: (_data, variables) => invalidate(variables.postId),
  })
}

export const useUnbookmarkMutation = (
  options: BookmarkMutationOptions = {},
) => {
  const { invalidate } = useInvalidateBookmarkQueries(options)

  return useMutation<void, Error, BookmarkCommand>({
    mutationFn: (command) => bookmarkApi.unbookmark(command),
    onSuccess: (_data, variables) => invalidate(variables.postId),
  })
}
