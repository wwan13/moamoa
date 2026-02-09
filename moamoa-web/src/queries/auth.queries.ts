import { useMutation } from "@tanstack/react-query"
import {
  authApi,
  type AuthTokens,
  type LoginCommand,
  type LoginSocialSessionCommand,
  type LogoutResult,
  type SignupCommand,
} from "../api/auth.api"

export const useLoginMutation = () => {
  return useMutation<AuthTokens, Error, LoginCommand>({
    mutationFn: (command) => authApi.login(command),
  })
}

export const useSignupMutation = () => {
  return useMutation<AuthTokens, Error, SignupCommand>({
    mutationFn: (command) => authApi.signup(command),
  })
}

export const useLogoutMutation = () => {
  return useMutation<LogoutResult, Error, void>({
    mutationFn: () => authApi.logout(),
  })
}

export const useLoginSocialSessionMutation = () => {
  return useMutation<AuthTokens, Error, LoginSocialSessionCommand>({
    mutationFn: (command) => authApi.loginSocialSession(command),
  })
}
