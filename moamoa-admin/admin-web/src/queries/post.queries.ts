import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
    postApi,
    type AdminUpdateCategoryResult,
    type AdminPostList,
    type AdminPostQueryConditions,
} from "../api/post.api"

export function usePostsQuery(conditions: AdminPostQueryConditions) {
    return useQuery<AdminPostList>({
        queryKey: ["admin-posts", conditions],
        queryFn: () => postApi.findByConditions(conditions),
    })
}

type UpdatePostCategoryVariables = {
    postId: number
    categoryId: number
}

export function useUpdatePostCategoryMutation() {
    const queryClient = useQueryClient()

    return useMutation<AdminUpdateCategoryResult, Error, UpdatePostCategoryVariables>({
        mutationFn: ({ postId, categoryId }) =>
            postApi.updateCategory(postId, { categoryId }),
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: ["admin-posts"] })
        },
    })
}
