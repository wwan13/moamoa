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

/* 이메일 인증 코드 전송 */
export function useEmailVerificationMutation() {
    return useMutation({
        mutationFn: authApi.emailVerification,
        onSuccess: () => {
            showToast("인증번호가 전송되었습니다.", { type: "success" })
        },
        onError: async () => {
            await showGlobalAlert({
                title: "실패",
                message: "이미 사용 중인 이메일입니다.",
            })
        },
    })
}

/* 이메일 인증 코드 확인 */
export function useEmailVerificationConfirmMutation() {
    return useMutation({
        mutationFn: authApi.emailVerificationConfirm,
        onSuccess: () => {
            showToast("인증되었습니다.", { type: "success" })
        },
        onError: async () => {
            await showGlobalAlert({
                title: "실패",
                message: "인증번호가 올바르지 않습니다.",
            })
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