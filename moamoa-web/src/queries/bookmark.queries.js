import { useMutation, useQueryClient } from "@tanstack/react-query"
import { bookmarkApi } from "../api/bookmark.api.js"

export function useBookmarkToggleMutation() {
    const qc = useQueryClient()

    return useMutation({
        mutationFn: bookmarkApi.toggle,
        onSuccess: (_data, variables) => {
            // 리스트/상세 둘 다 있을 수 있어서, 보수적으로 invalidate
            qc.invalidateQueries({ queryKey: ["posts"] })
            qc.invalidateQueries({ queryKey: ["post", variables.postId] })
            qc.invalidateQueries({ queryKey: ["bookmarks"] })
        },
    })
}