import { useQuery } from "@tanstack/react-query"
import { techBlogApi } from "../api/techBlog.api.js"
import useAuth from "../auth/AuthContext.jsx";

export function useTechBlogsQuery() {
    const { authScope, publicScope } = useAuth()
    const scope = authScope ?? publicScope

    return useQuery({
        queryKey: ["techBlogs", scope],
        queryFn: ({ signal }) => techBlogApi.list({ signal }),
        staleTime: 60 * 1000,
    })
}

export function useTechBlogByKeyQuery({ key }) {
    const { authScope, publicScope } = useAuth()
    const scope = authScope ?? publicScope

    return useQuery({
        queryKey: ["techBlog", scope, key],
        queryFn: ({ signal }) => techBlogApi.findByKey({ key }, { signal }),
        enabled: !!key,
    })
}

export function useSubscribingTechBlogsQuery() {
    const { authScope } = useAuth()

    return useQuery({
        queryKey: ["techBlogs", "subscribed", authScope],
        queryFn: ({ signal }) => techBlogApi.listSubscribed({signal}),
        enabled: !!authScope,      // ✅ 로그인일 때만
        staleTime: 10 * 1000,
    })
}