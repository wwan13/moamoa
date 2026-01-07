import {apiRequest} from "./client.js";

export function categoryApi(onError) {
    return apiRequest(
        "/api/category",
        {
            method: "GET",
        },
        {
            onError: onError ?? ((err) => {
            }),
        }
    )
}