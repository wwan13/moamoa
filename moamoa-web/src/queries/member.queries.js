import useAuth from "../auth/AuthContext.jsx";
import {useMutation, useQuery} from "@tanstack/react-query";
import {memberApi} from "../api/member.api.js";

export function useMemberSummaryQuery() {
    const { authScope } = useAuth()

    return useQuery({
        queryKey: ["member", authScope],
        queryFn: ({ signal }) => memberApi.summary({ signal }),
        // enabled: !!authScope
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
