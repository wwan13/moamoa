import { useQuery } from "@tanstack/react-query"
import { techBlogApi } from "../api/techBlog.api.js"
import useAuth from "../auth/AuthContext.jsx";

export function useTechBlogsQuery({ query } = {}, options = {}) {
    const { authScope, publicScope } = useAuth()
    const scope = authScope ?? publicScope

    const hasQuery = !!query

    return useQuery({
        queryKey: ["techBlogs", scope, { query: hasQuery ? query : undefined }],
        queryFn: ({ signal }) => techBlogApi.list({ query }, { signal }),
        enabled: options.enabled ?? true,

        staleTime: hasQuery ? 0 : 60 * 1000,
        gcTime: hasQuery ? 0 : 5 * 60 * 1000,
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