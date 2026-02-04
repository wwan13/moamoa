import useAuth from "../auth/AuthContext.jsx";
import {useMutation, useQuery} from "@tanstack/react-query";
import {memberApi} from "../api/member.api.js";

export function useMemberSummaryQuery() {
    const { authScope } = useAuth()

    return useQuery({
        queryKey: ["member", "summary", authScope?.id ?? authScope ?? "none"],
        queryFn: ({ signal }) => memberApi.summary({ signal }),
        enabled: !!authScope,
        staleTime: 0,
    })
}

export function useCreateSocialMemberMutation() {
    return useMutation({
        mutationFn: memberApi.createSocial,
        onError: async () => {
        },
    })
}

export function useChangePasswordMutation() {
    return useMutation({
        mutationFn: memberApi.changePassword,
        onError: async () => {
        },
    })
}

export function useUnjoinMutation() {
    return useMutation({
        mutationFn: memberApi.unjoin,
        onError: async () => {
        },
    })
}
