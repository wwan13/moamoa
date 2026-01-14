import { useQuery } from "@tanstack/react-query"
import { categoryApi } from "../api/category.api.js"

export function useCategoryQuery() {
    return useQuery({
        queryKey: ["categories"],
        queryFn: ({ signal }) => categoryApi.list({ signal }),
        staleTime: 60 * 60 * 1000, // 카테고리는 거의 안 바뀜
    })
}