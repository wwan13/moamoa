import { useQuery } from "@tanstack/react-query"
import { categoryApi } from "../api/category.api.js"
import useAuth from "../auth/AuthContext.jsx"

export function useCategoryQuery() {
    const { publicScope } = useAuth()

    return useQuery({
        queryKey: ["categories", publicScope],
        queryFn: ({ signal }) => categoryApi.list({ signal }),
        staleTime: 60 * 60 * 1000, // 카테고리는 거의 안 바뀜
    })
}