import {apiRequest} from "./client.js";

export function postsApi({ page, techBlogId } = {}, onError) {
    const params = new URLSearchParams()

    if (page && page > 1) params.set("page", page)
    params.set("size", 20)

    if (techBlogId != null) params.set("techBlogId", techBlogId)

    const query = params.toString()
    const url = query ? `/api/post?${query}` : "/api/post"

    return apiRequest(
        url,
        { method: "GET" },
        { onError: onError ?? (() => {}) }
    )
}

export function postsViewCountApi(postId, onError) {
    return apiRequest(
        `/api/post/${postId}/view`,
        {
            method: "POST",
        },
        {
            onError: onError ?? (() => {}),
        }
    )
}