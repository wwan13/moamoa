// apiClient.js
const BASE_URL = import.meta.env.VITE_API_BASE_URL || ""

const ACCESS_TOKEN_KEY = "accessToken"
const REFRESH_TOKEN_KEY = "refreshToken"

let onLoginRequired = () => {}
export function setOnLoginRequired(handler) {
    onLoginRequired = typeof handler === "function" ? handler : () => {}
}

let onServerError = () => {}
export function setOnServerError(handler) {
    onServerError = typeof handler === "function" ? handler : () => {}
}

let onLogout = () => {}
export function setOnLogout(handler) {
    onLogout = typeof handler === "function" ? handler : () => {}
}

let onToast = () => {}
export function setOnToast(handler) {
    onToast = typeof handler === "function" ? handler : () => {}
}
export function showToast(message, options = {}) {
    const { type = "default", duration = 2000 } = options
    onToast({ message, type, duration })
}

let onGlobalAlert = () => {}
export function setOnGlobalAlert(handler) {
    onGlobalAlert = typeof handler === "function" ? handler : () => {}
}
export function showGlobalAlert(params) {
    const { message, title = "오류" } =
        typeof params === "string" ? { message: params } : params ?? {}

    return new Promise((resolve) => {
        onGlobalAlert({
            title,
            message: message ?? "알 수 없는 오류가 발생했어요.",
            onClose: resolve,
        })
    })
}

let onGlobalConfirm = () => {}
export function setOnGlobalConfirm(handler) {
    onGlobalConfirm = typeof handler === "function" ? handler : () => {}
}
export function showGlobalConfirm({
                                      message,
                                      title = "확인",
                                      confirmText = "확인",
                                      cancelText = "취소",
                                  } = {}) {
    return new Promise((resolve) => {
        onGlobalConfirm({
            title,
            message: message ?? "계속 진행할까요?",
            confirmText,
            cancelText,
            onConfirm: () => resolve(true),
            onCancel: () => resolve(false),
        })
    })
}

function getAccessToken() {
    return localStorage.getItem(ACCESS_TOKEN_KEY)
}
function setAccessToken(token) {
    localStorage.setItem(ACCESS_TOKEN_KEY, token)
}
function getRefreshToken() {
    return localStorage.getItem(REFRESH_TOKEN_KEY)
}

// 204 대응
async function safeJson(res) {
    const text = await res.text()
    if (!text) return null
    try {
        return JSON.parse(text)
    } catch {
        return null
    }
}

export class ApiError extends Error {
    constructor({ status, message, body }) {
        super(message)
        this.name = "ApiError"
        this.status = status
        this.body = body
    }
}

let isRefreshing = false
let refreshPromise = null

async function reissueToken(baseUrl = "") {
    const refreshToken = getRefreshToken()
    if (!refreshToken) throw new Error("NO_REFRESH_TOKEN")

    try {
        const res = await fetch(`${baseUrl}/api/auth/reissue`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-Refresh-Token": refreshToken,
            },
        })

        if (!res.ok) throw new Error("REISSUE_FAILED")

        const data = await safeJson(res)
        if (!data?.accessToken) throw new Error("INVALID_REISSUE_RESPONSE")

        setAccessToken(data.accessToken)
        if (data.refreshToken) {
            localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
        }
        return data.accessToken
    } catch (e) {
        onLogout()
        throw e
    }
}

async function ensureReissued(baseUrl) {
    if (isRefreshing && refreshPromise) return refreshPromise

    isRefreshing = true
    refreshPromise = (async () => {
        try {
            return await reissueToken(baseUrl)
        } finally {
            isRefreshing = false
            refreshPromise = null
        }
    })()

    return refreshPromise
}

/**
 * React Query에서 쓰기 좋은 fetcher
 * - queryFn/mutationFn에 바로 넣을 수 있게 구성
 */
export async function apiRequest(path, options = {}, config = {}) {
    const {
        baseUrl = BASE_URL,
        retry = true, // TOKEN_EXPIRED일 때 1회 재시도
        signal, // react-query에서 abort 전달 가능
    } = config

    const headers = new Headers(options.headers || {})
    headers.set("Accept", "application/json")

    // body가 있고 FormData가 아니면 JSON 기본
    if (options.body && !(options.body instanceof FormData) && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json")
    }

    const token = getAccessToken()
    if (token) headers.set("Authorization", `Bearer ${token}`)

    const res = await fetch(`${baseUrl}${path}`, {
        ...options,
        headers,
        signal,
    })

    if (res.ok) return await safeJson(res)

    const errBody = (await safeJson(res)) || { status: res.status, message: "UNKNOWN" }
    const status = errBody.status ?? res.status
    const message = errBody.message ?? "UNKNOWN"

    // 로그인 다시 필요
    if (status === 401 && message === "LOGIN_AGAIN") {
        onLoginRequired()
        throw new ApiError({ status, message, body: errBody })
    }

    // 토큰 만료 → 1회 재발급 후 재요청
    if (status === 401 && message === "TOKEN_EXPIRED") {
        if (!retry) {
            onLoginRequired()
            throw new ApiError({ status, message: "LOGIN_AGAIN", body: errBody })
        }

        await ensureReissued(baseUrl)

        // ✅ 재요청은 retry:false로 한번만
        return await apiRequest(path, options, { ...config, retry: false })
    }

    if (status === 500) onServerError({ message: "잠시 후 다시 시도해 주세요." })

    throw new ApiError({ status, message, body: errBody })
}

/**
 * 편의 함수들: 컴포넌트/훅에서 더 짧게 쓰기
 */
export const http = {
    get: (path, config = {}) => apiRequest(path, { method: "GET" }, config),
    post: (path, body, config = {}) =>
        apiRequest(path, { method: "POST", body: body instanceof FormData ? body : JSON.stringify(body) }, config),
    put: (path, body, config = {}) =>
        apiRequest(path, { method: "PUT", body: body instanceof FormData ? body : JSON.stringify(body) }, config),
    patch: (path, body, config = {}) =>
        apiRequest(path, { method: "PATCH", body: body instanceof FormData ? body : JSON.stringify(body) }, config),
    del: (path, config = {}) => apiRequest(path, { method: "DELETE" }, config),
}