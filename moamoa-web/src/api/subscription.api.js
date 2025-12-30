import {apiRequest} from "./client.js";

export function subscribingBlogsApi(onError) {
    return apiRequest(
        "/api/tech-blog-subscription",
        {
            method: "GET",
        },
        {
            onError: onError ?? ((err) => {
            }),
        }
    )
}

export function subscriptionToggleApi(techBlogId, onError) {
    return apiRequest(
        "/api/tech-blog-subscription",
        {
            method: "POST",
            body: JSON.stringify({ techBlogId})
        },
        {
            onError: onError ?? ((err) => {
            }),
        }
    )
}