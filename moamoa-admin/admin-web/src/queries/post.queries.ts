import { useQuery } from "@tanstack/react-query"
import {
    postApi,
    type AdminPostList,
    type AdminPostQueryConditions,
} from "../api/post.api"

export function usePostsQuery(conditions: AdminPostQueryConditions) {
    return useQuery<AdminPostList>({
        queryKey: ["admin-posts", conditions],
        queryFn: () => postApi.findByConditions(conditions),
    })
}

