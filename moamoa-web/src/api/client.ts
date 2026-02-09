const BASE_URL = import.meta.env.VITE_API_BASE_URL || ""

const ACCESS_TOKEN_KEY = "accessToken"
const REFRESH_TOKEN_KEY = "refreshToken"

export const authStorageKeys = {
  accessToken: ACCESS_TOKEN_KEY,
  refreshToken: REFRESH_TOKEN_KEY,
} as const

export type ToastType = "default" | "success" | "error" | "warning" | "info"

export type Toast = {
  message: string
  type: ToastType
  duration: number
}

export type ToastOptions = {
  type?: ToastType
  duration?: number
}

export type ServerErrorParams = {
  message: string
}

export type GlobalAlertParams = {
  title?: string
  message?: string
  onClose?: () => void
}

export type GlobalConfirmParams = {
  title?: string
  message?: string
  confirmText?: string
  cancelText?: string
  onConfirm?: () => void
  onCancel?: () => void
}

export type GlobalConfirmOptions = {
  message?: string
  title?: string
  confirmText?: string
  cancelText?: string
}

type SearchHandler = () => void

let onLoginRequired: () => void = () => {}
export const setOnLoginRequired = (handler: () => void): void => {
  onLoginRequired = typeof handler === "function" ? handler : () => {}
}

let onServerError: (params: ServerErrorParams) => void = () => {}
export const setOnServerError = (handler: (params: ServerErrorParams) => void): void => {
  onServerError = typeof handler === "function" ? handler : () => {}
}

let onLogout: () => void = () => {}
export const setOnLogout = (handler: () => void): void => {
  onLogout = typeof handler === "function" ? handler : () => {}
}

let onToast: (toast: Toast) => void = () => {}
export const setOnToast = (handler: (toast: Toast) => void): void => {
  onToast = typeof handler === "function" ? handler : () => {}
}

export const showToast = (message: string, options: ToastOptions = {}): void => {
  onToast({
    message,
    type: options.type ?? "default",
    duration: options.duration ?? 1500,
  })
}

let onGlobalAlert: (params: GlobalAlertParams) => void = () => {}
export const setOnGlobalAlert = (handler: (params: GlobalAlertParams) => void): void => {
  onGlobalAlert = typeof handler === "function" ? handler : () => {}
}

export const showGlobalAlert = (
  params: string | Omit<GlobalAlertParams, "onClose">
): Promise<void> => {
  const { message, title = "오류" } =
    typeof params === "string" ? { message: params } : params ?? {}

  return new Promise<void>((resolve) => {
    onGlobalAlert({
      title,
      message: message ?? "알 수 없는 오류가 발생했어요.",
      onClose: resolve,
    })
  })
}

let onGlobalConfirm: (params: GlobalConfirmParams) => void = () => {}
export const setOnGlobalConfirm = (handler: (params: GlobalConfirmParams) => void): void => {
  onGlobalConfirm = typeof handler === "function" ? handler : () => {}
}

export const showGlobalConfirm = ({
  message,
  title = "확인",
  confirmText = "확인",
  cancelText = "취소",
}: GlobalConfirmOptions = {}): Promise<boolean> => {
  return new Promise<boolean>((resolve) => {
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

let onOpenSearch: SearchHandler = () => {}
let onCloseSearch: SearchHandler = () => {}

export const setOnOpenSearch = (handler: SearchHandler): void => {
  onOpenSearch = typeof handler === "function" ? handler : () => {}
}

export const setOnCloseSearch = (handler: SearchHandler): void => {
  onCloseSearch = typeof handler === "function" ? handler : () => {}
}

export const openSearch = (): void => {
  onOpenSearch()
}

export const closeSearch = (): void => {
  onCloseSearch()
}

const getAccessToken = (): string | null => localStorage.getItem(ACCESS_TOKEN_KEY)
const setAccessToken = (token: string): void => localStorage.setItem(ACCESS_TOKEN_KEY, token)
const getRefreshToken = (): string | null => localStorage.getItem(REFRESH_TOKEN_KEY)

const safeJson = async <T = unknown>(res: Response): Promise<T | null> => {
  const text = await res.text()
  if (!text) return null

  try {
    return JSON.parse(text) as T
  } catch {
    return null
  }
}

export class ApiError extends Error {
  status: number
  body: unknown

  constructor({ status, message, body }: { status: number; message: string; body: unknown }) {
    super(message)
    this.name = "ApiError"
    this.status = status
    this.body = body
  }
}

type ErrorBody = {
  status?: number
  message?: string
}

let isRefreshing = false
let refreshPromise: Promise<string> | null = null

const reissueToken = async (baseUrl = ""): Promise<string> => {
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

    const data = await safeJson<{ accessToken?: string; refreshToken?: string }>(res)
    if (!data?.accessToken) throw new Error("INVALID_REISSUE_RESPONSE")

    setAccessToken(data.accessToken)
    if (data.refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
    }

    return data.accessToken
  } catch (error) {
    onLogout()
    throw error
  }
}

const ensureReissued = async (baseUrl: string): Promise<string> => {
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

export type ApiRequestConfig = {
  baseUrl?: string
  retry?: boolean
  signal?: AbortSignal
}

export const apiRequest = async <T = unknown>(
  path: string,
  options: RequestInit = {},
  config: ApiRequestConfig = {}
): Promise<T | null> => {
  const { baseUrl = BASE_URL, retry = true, signal } = config

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
    signal,
  })

  if (res.ok) return await safeJson<T>(res)

  const errBody = (await safeJson<ErrorBody>(res)) ?? { status: res.status, message: "UNKNOWN" }
  const status = errBody.status ?? res.status
  const message = errBody.message ?? "UNKNOWN"

  if (status === 401 && message === "LOGIN_AGAIN") {
    onLoginRequired()
    throw new ApiError({ status, message, body: errBody })
  }

  if (status === 401 && message === "TOKEN_EXPIRED") {
    if (!retry) {
      onLoginRequired()
      throw new ApiError({ status, message: "LOGIN_AGAIN", body: errBody })
    }

    await ensureReissued(baseUrl)
    return await apiRequest<T>(path, options, { ...config, retry: false })
  }

  if (status === 500) {
    onServerError({ message: "잠시 후 다시 시도해 주세요." })
  }

  throw new ApiError({ status, message, body: errBody })
}

export const http = {
  get: <T = unknown>(path: string, config: ApiRequestConfig = {}): Promise<T | null> =>
    apiRequest<T>(path, { method: "GET" }, config),
  post: <T = unknown>(path: string, body: unknown, config: ApiRequestConfig = {}): Promise<T | null> =>
    apiRequest<T>(path, { method: "POST", body: body instanceof FormData ? body : JSON.stringify(body) }, config),
  put: <T = unknown>(path: string, body: unknown, config: ApiRequestConfig = {}): Promise<T | null> =>
    apiRequest<T>(path, { method: "PUT", body: body instanceof FormData ? body : JSON.stringify(body) }, config),
  patch: <T = unknown>(path: string, body: unknown, config: ApiRequestConfig = {}): Promise<T | null> =>
    apiRequest<T>(path, { method: "PATCH", body: body instanceof FormData ? body : JSON.stringify(body) }, config),
  del: <T = unknown>(path: string, config: ApiRequestConfig = {}): Promise<T | null> =>
    apiRequest<T>(path, { method: "DELETE" }, config),
}
