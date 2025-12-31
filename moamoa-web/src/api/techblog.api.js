import {apiRequest, showGlobalAlert} from "./client.js";

export function techBlogsApi(onError) {
    return apiRequest(
        "/api/tech-blog",
        {
            method: "GET",
        },
        {
            onError: onError ?? ((err) => {
            }),
        }
    )
}

export function findByTechBlogKeyApi(key, onError) {
    return apiRequest(
        `/api/tech-blog/${key}`,
        {
            method: "GET",
        },
        {
            onError: onError ?? ((err) => {
            }),
        }
    )
}