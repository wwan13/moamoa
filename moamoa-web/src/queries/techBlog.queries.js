import { useQuery } from "@tanstack/react-query"
import { techBlogApi } from "../api/techBlog.api.js"

export function useTechBlogsQuery() {
    return useQuery({
        queryKey: ["techBlogs"],
        queryFn: ({ signal }) => techBlogApi.list({ signal }),
        staleTime: 60 * 1000,
    })
}

export function useTechBlogByKeyQuery({ key }) {
    return useQuery({
        queryKey: ["techBlog", key],
        queryFn: ({ signal }) => techBlogApi.findByKey({ key }, { signal }),
        enabled: !!key,
    })
}

export function useSubscribingTechBlogsQuery() {
    return useQuery({
        queryKey: ["techBlogs", "subscribed"],
        queryFn: ({ signal }) => techBlogApi.listSubscribed({ signal }),
        staleTime: 10 * 1000,
    })
}