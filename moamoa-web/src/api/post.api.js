import {apiRequest} from "./client.js";

export function postsApi({ page } = {}, onError) {
    const params = new URLSearchParams()

    if (page && page > 1) params.set("page", page)
    params.set("size", 20)

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

export function postsByTechBlogKeyApi({ page, techBlogKey } = {}, onError) {
    const params = new URLSearchParams()

    if (page && page > 1) params.set("page", page)
    params.set("size", 20)
    params.set("techBlogKey", techBlogKey)

    const query = params.toString()
    const url = query ? `/api/post/tech-blog?${query}` : "/api/post/tech-blog"

    return apiRequest(
        url,
        { method: "GET" },
        { onError: onError ?? (() => {}) }
    )
}

export function postsBySubscriptionApi({ page } = {}, onError) {
    const params = new URLSearchParams()

    if (page && page > 1) params.set("page", page)
    params.set("size", 20)

    const query = params.toString()
    const url = query ? `/api/post/subscription?${query}` : "/api/post/subscription"

    return apiRequest(
        url,
        { method: "GET" },
        { onError: onError ?? (() => {}) }
    )
}

export function postsByBookmarkApi({ page } = {}, onError) {
    const params = new URLSearchParams()

    if (page && page > 1) params.set("page", page)
    params.set("size", 20)

    const query = params.toString()
    const url = query ? `/api/post/bookmark?${query}` : "/api/post/bookmark"

    return apiRequest(
        url,
        { method: "GET" },
        { onError: onError ?? (() => {}) }
    )
}