import { useMutation, useQuery } from "@tanstack/react-query"
import useAuth from "../auth/useAuth"
import {
  memberApi,
  type ChangePasswordCommand,
  type CreateSocialMemberCommand,
  type CreateSocialMemberResult,
  type MemberSummary,
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
  return useMutation<
    CreateSocialMemberResult,
    Error,
    CreateSocialMemberCommand
  >({
    mutationFn: (command) => memberApi.createSocial(command),
  })
}

export const useChangePasswordMutation = () => {
  return useMutation<void, Error, ChangePasswordCommand>({
    mutationFn: (command) => memberApi.changePassword(command),
  })
}

export const useUnjoinMutation = () => {
  return useMutation<void, Error, void>({
    mutationFn: () => memberApi.unjoin(),
  })
}
