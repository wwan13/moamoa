import {apiRequest, showGlobalAlert} from "./client.js";

export function loginApi(email, password, onError) {
    return apiRequest(
        "/api/auth/login",
        {
            method: "POST",
            body: JSON.stringify({ email, password }),
        },
        {
            onError: onError ?? ((err) => {
            }),
        }
    )
}

export function emailVerificationApi(email, onError) {
    return apiRequest(
        "/api/auth/email-verification",
        {
            method: "POST",
            body: JSON.stringify({ email }),
        },
        {
            onError: onError ?? ((err) => {
            }),
        }
    )
}

export function emailVerificationConfirmApi(email, code, onError) {
    return apiRequest(
        "/api/auth/email-verification/confirm",
        {
            method: "POST",
            body: JSON.stringify({
                email,
                code: String(code),
            }),
        },
        {
            onError: onError ?? ((err) => {
            }),
        }
    )
}

export function logoutApi(onError) {
    return apiRequest(
        "/api/auth/logout",
        {
            method: "POST"
        },
        {
            onError: onError ?? ((err) => {
            }),
        }
    )
}