import { useMutation, useQuery } from "@tanstack/react-query"
import useAuth from "../auth/useAuth"
import {
  memberApi,
  type ChangePasswordCommand,
  type ChangePasswordResult,
  type CreateSocialMemberCommand,
  type CreateSocialMemberResult,
  type MemberSummary,
  type MemberUnjoinResult,
} from "../api/member.api"

export const useMemberSummaryQuery = () => {
  const { authScope } = useAuth()

  return useQuery<MemberSummary | null>({
    queryKey: ["member", "summary", authScope ?? "none"],
    queryFn: ({ signal }) => memberApi.summary({ signal }),
    enabled: !!authScope,
    staleTime: 0,
  })
}

export const useCreateSocialMemberMutation = () => {
  return useMutation<CreateSocialMemberResult, Error, CreateSocialMemberCommand>({
    mutationFn: (command) => memberApi.createSocial(command),
  })
}

export const useChangePasswordMutation = () => {
  return useMutation<ChangePasswordResult, Error, ChangePasswordCommand>({
    mutationFn: (command) => memberApi.changePassword(command),
  })
}

export const useUnjoinMutation = () => {
  return useMutation<MemberUnjoinResult, Error, void>({
    mutationFn: () => memberApi.unjoin(),
  })
}
