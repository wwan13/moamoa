import {apiRequest} from "./client.js";

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