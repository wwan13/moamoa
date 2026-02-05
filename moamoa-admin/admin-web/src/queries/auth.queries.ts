import { useMutation } from "@tanstack/react-query"
import { authApi } from "../api/auth.api"

export function useLoginMutation() {
    return useMutation({
        mutationFn: authApi.login,
    })
}

export function useLogoutMutation() {
    return useMutation({
        mutationFn: authApi.logout,
    })
}
