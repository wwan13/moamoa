import useAuth from "../auth/AuthContext.jsx";
import {useNavigate} from "react-router-dom";

const BASE_URL = import.meta.env.VITE_API_BASE_URL || ""

const ACCESS_TOKEN_KEY = "accessToken"
const REFRESH_TOKEN_KEY = "refreshToken" // 너가 쓰는 키로 맞춰

let onLoginRequired = () => {} // 외부에서 주입

let onLoadingChange = () => {}

export function setOnLoadingChange(handler) {
    onLoadingChange = typeof handler === "function" ? handler : () => {}
}

let onServerError = () => {}

export function setOnServerError(handler) {
    onServerError = typeof handler === "function" ? handler : () => {}
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
            onClose: resolve, // ✅ 닫힐 때 resolve
        })
    })
}

let onToast = () => {}

export function setOnToast(handler) {
    onToast = typeof handler === "function" ? handler : () => {}
}

export function showToast(message, options = {}) {
    const {
        type = "default", // default | success | error
        duration = 2000,
    } = options
    onToast({ message, type, duration })
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

let isRefreshing = false
let refreshPromise = null

export function setOnLoginRequired(handler) {
    onLoginRequired = typeof handler === "function" ? handler : () => {}
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

// 서버가 204(바디 없음) 줄 수도 있어서 안전하게 처리
async function safeJson(res) {
    const text = await res.text()
    if (!text) return null
    try {
        return JSON.parse(text)
    } catch {
        return null
    }
}

let onLogout = () => {}

export function setOnLogout(handler) {
    onLogout = typeof handler === "function" ? handler : () => {}
}

/**
 * Reissue API 호출
 * - 너의 백엔드 스펙에 맞게 URL/바디/응답을 수정해줘.
 * - 여기서는 refreshToken을 바디로 보내고 accessToken을 받는 형태로 가정.
 */
async function reissueToken(baseUrl = "") {
    const refreshToken = getRefreshToken()
    if (!refreshToken) {
        await showGlobalAlert("다시 로그인해 주세요.")
        localStorage.removeItem(ACCESS_TOKEN_KEY)
        localStorage.removeItem(REFRESH_TOKEN_KEY)
        window.location.reload()
        return
    }

    try {
        const res = await fetch(`${baseUrl}/api/auth/reissue`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-Refresh-Token": refreshToken,
            },
        })

        if (!res.ok) {
            await showGlobalAlert("다시 로그인해 주세요.")
            localStorage.removeItem(ACCESS_TOKEN_KEY)
            localStorage.removeItem(REFRESH_TOKEN_KEY)
            window.location.reload()
            return
        }

        const data = await safeJson(res)
        if (!data?.accessToken) {
            await showGlobalAlert("다시 로그인해 주세요.")
            localStorage.removeItem(ACCESS_TOKEN_KEY)
            localStorage.removeItem(REFRESH_TOKEN_KEY)
            window.location.reload()
            return
        }

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

export class ApiError extends Error {
    constructor({ status, message }) {
        super(message)
        this.status = status
        this.message = message
    }
}

/**
 * 공통 API 함수
 */
const LOADING_DELAY_MS = 200 // 이 시간보다 오래 걸리면 로딩 켬

export async function apiRequest(path, options = {}, config = {}) {
    const {
        retry = true,
        onError,
        baseUrl = BASE_URL,
        showLoading = true,
    } = config

    let loadingTimer = null
    let loadingShown = false

    if (showLoading) {
        loadingTimer = setTimeout(() => {
            loadingShown = true
            onLoadingChange(true)
        }, LOADING_DELAY_MS)
    }

    try {
        const headers = new Headers(options.headers || {})
        headers.set("Accept", "application/json")

        if (options.body && !(options.body instanceof FormData) && !headers.has("Content-Type")) {
            headers.set("Content-Type", "application/json")
        }

        const token = getAccessToken()
        if (token) headers.set("Authorization", `Bearer ${token}`)

        const res = await fetch(`${baseUrl}${path}`, {
            ...options,
            headers,
        })

        if (res.ok) return await safeJson(res)

        const errBody = (await safeJson(res)) || { status: res.status, message: "UNKNOWN" }
        const status = errBody.status ?? res.status
        const message = errBody.message ?? "UNKNOWN"

        if (status === 401 && message === "LOGIN_AGAIN") {
            onLoginRequired()
            const err = new ApiError({ status, message })
            onError?.(err)
            throw err
        }

        if (status === 401 && message === "TOKEN_EXPIRED") {
            if (!retry) {
                onLoginRequired()
                const err = new ApiError({ status, message: "LOGIN_AGAIN" })
                onError?.(err)
                throw err
            }

            try {
                await ensureReissued(baseUrl)
            } catch (e) {
                onServerError({ message: "잠시 후 다시 시도해 주세요." })
                throw e
            }

            return await apiRequest(path, options, { ...config, retry: false })
        }

        if (status === 500) onServerError({ message: "잠시 후 다시 시도해 주세요." })

        const err = new ApiError({ status, message })
        onError?.(err)
        throw err
    } finally {
        if (loadingTimer) clearTimeout(loadingTimer)
        if (loadingShown) onLoadingChange(false)
    }
}