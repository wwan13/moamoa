import {apiRequest} from "./client.js";

export function bookmarkToggleApi(postId, onError) {
    return apiRequest(
        "/api/post-bookmark",
        {
            method: "POST",
            body: JSON.stringify({ postId })
        },
        {
            onError: onError ?? ((err) => {
            }),
        }
    )
}