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