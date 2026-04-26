import { useQuery } from "@tanstack/react-query"
import { categoryApi, type CategoryData } from "../api/category.api"
import useAuth from "../auth/useAuth"

export const useCategoryQuery = () => {
  const { publicScope } = useAuth()

  return useQuery<CategoryData[]>({
    queryKey: ["categories", publicScope],
    queryFn: ({ signal }) => categoryApi.list({ signal }),
    staleTime: 60 * 60 * 1000,
  })
}
