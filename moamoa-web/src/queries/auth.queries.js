import { useMutation } from "@tanstack/react-query"
import { authApi } from "../api/auth.api.js"
import { showGlobalAlert, showToast } from "../api/client.js"

/* 로그인 */
export function useLoginMutation() {
    return useMutation({
        mutationFn: authApi.login,
        onError: async () => {
        },
    })
}

/* 회원가입 */
export function useSignupMutation() {
    return useMutation({
        mutationFn: authApi.signup,
        onSuccess: () => {
            showToast("회원가입이 완료되었습니다.", { type: "success" })
        },
        onError: async () => {
        },
    })
}

/* 로그아웃 */
export function useLogoutMutation() {
    return useMutation({
        mutationFn: authApi.logout,
        onSuccess: () => {
            showToast("로그아웃 되었습니다.", { type: "success" })
        },
    })
}