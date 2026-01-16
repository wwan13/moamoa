import { useMutation, useQueryClient } from "@tanstack/react-query"
import { bookmarkApi } from "../api/bookmark.api.js"
import useAuth from "../auth/AuthContext.jsx"

export function useBookmarkToggleMutation(options = {}) {
    const qc = useQueryClient()
    const { authScope, publicScope } = useAuth()
    const scope = authScope ?? publicScope

    const invalidateOnSuccess = options.invalidateOnSuccess ?? true

    return useMutation({
        mutationFn: bookmarkApi.toggle,
        onSuccess: (_data, variables) => {
            if (!invalidateOnSuccess) return

            qc.invalidateQueries({ queryKey: ["posts", scope] })
            qc.invalidateQueries({ queryKey: ["post", scope, variables.postId] })
        },
    })
}